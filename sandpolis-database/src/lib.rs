#![feature(iterator_try_collect)]
#![feature(lock_value_accessors)]

use crate::config::DatabaseConfig;
use anyhow::{Result, anyhow, bail};
use chrono::{DateTime, Utc};
use native_db::db_type::{KeyDefinition, KeyEntry, KeyOptions, KeyRange, ToKeyDefinition};
use native_db::transaction::RTransaction;
use native_db::transaction::query::{SecondaryScan, SecondaryScanIterator};
use native_db::watch::Event;
use native_db::{Key, Models, ToInput, ToKey};
use sandpolis_core::{InstanceId, RealmName};
use serde::{Deserialize, Serialize};
use std::collections::{BTreeMap, HashMap};
use std::ops::{Range, RangeBounds, RangeFrom};
use std::path::Path;
use std::sync::atomic::AtomicU64;
use std::{marker::PhantomData, sync::Arc};
use tokio::sync::{RwLock, RwLockReadGuard, RwLockWriteGuard};
use tokio_util::sync::CancellationToken;
use tracing::instrument::WithSubscriber;
use tracing::{debug, trace};

pub mod config;

#[derive(Clone)]
pub struct DatabaseLayer {
    config: DatabaseConfig,
    models: &'static Models,
    inner: Arc<RwLock<HashMap<RealmName, RealmDatabase>>>,
}

impl DatabaseLayer {
    pub fn new(config: DatabaseConfig, models: &'static Models) -> Result<Self> {
        let default = if let Some(path) = config.get_storage_dir()? {
            let path = path.join("default.db");

            debug!(path = %path.display(), "Initializing persistent default database");
            native_db::Builder::new().create(models, path)?
        } else {
            debug!("Initializing ephemeral default database");
            native_db::Builder::new().create_in_memory(models)?
        };

        Ok(Self {
            config,
            models,
            inner: Arc::new(RwLock::new(HashMap::from([(
                RealmName::default(),
                RealmDatabase::new(default),
            )]))),
        })
    }

    /// Get a `RealmDatabase` for the given realm.
    pub async fn realm(&mut self, name: RealmName) -> Result<Arc<RealmDatabase>> {
        {
            let inner = self.inner.read().await;
            if let Some(db) = inner.get(&name) {
                return Ok(db.clone());
            }
        }

        let mut inner = self.inner.write().await;

        let db = if let Some(path) = self.config.get_storage_dir()? {
            let path = path.join(format!("{name}.db"));

            debug!(realm = %name, path = %path.display(), "Initializing persistent realm database");
            Arc::new(RealmDatabase::new(
                native_db::Builder::new().create(self.models, path)?,
            ))
        } else {
            debug!(realm = %name, "Initializing ephemeral realm database");
            Arc::new(RealmDatabase::new(
                native_db::Builder::new().create_in_memory(self.models)?,
            ))
        };
        inner.insert(name, db.clone());

        Ok(db)
    }
}

/// Database containing all `Data` for a particular realm.
#[derive(Clone)]
pub struct RealmDatabase(Arc<native_db::Database<'static>>);

impl RealmDatabase {
    fn new(inner: native_db::Database<'static>) -> Self {
        Self(Arc::new(inner))
    }

    pub fn singleton() {}

    pub fn query<T: Data>(&self) -> ResidentVecBuilder<T> {
        ResidentVecBuilder {
            db: self.0.clone(),
            _phantom: PhantomData,
            conditions: Vec::new(),
        }
    }
}

// TODO if we define update, etc on these traits and pass in the db as a
// parameter, we can use dynamic dispatch to handle different impl for versioned
// Data instead of separate helper struct

/// `Data` is what's stored in a database!
pub trait Data
where
    Self: ToInput + Clone + PartialEq + Send + Sync + Default,
{
    fn id(&self) -> DataIdentifier;
    // fn set_id(&mut self, id: DataIdentifier);
}

/// `Data` that maintains a history of its value over time.
pub trait HistoricalData
where
    Self: Data,
{
    fn timestamp(&self) -> DbTimestamp;
    // fn set_timestamp(&self, timestamp: DbTimestamp);

    fn timestamp_key() -> KeyDefinition<KeyOptions> {
        KeyDefinition::new(
            Self::native_model_id(),
            Self::native_model_version(),
            "_timestamp",
            <String>::key_names(),
            KeyOptions {
                unique: false,
                optional: false,
            },
        )
    }
}

/// `Data` that expires and will be removed from the database after a certain
/// time.
pub trait ExpiringData
where
    Self: Data,
{
    fn expiration(&self) -> DataExpiration;
}

/// `Data` that is specific to a particular instance.
pub trait InstanceData
where
    Self: Data,
{
    fn instance_id(&self) -> InstanceId;
}

// TODO Eq based only on inner?
#[derive(Serialize, Deserialize, Eq, PartialEq, Debug, Clone, Copy, Hash)]
pub enum DbTimestamp {
    Latest(DateTime<Utc>),
    Previous(DateTime<Utc>),
}

impl Default for DbTimestamp {
    fn default() -> Self {
        Self::Latest(DateTime::default())
    }
}

impl ToKey for DbTimestamp {
    fn to_key(&self) -> native_db::Key {
        native_db::Key::new(
            match self {
                DbTimestamp::Latest(dt) => dt,
                DbTimestamp::Previous(dt) => dt,
            }
            .timestamp_millis()
            .to_be_bytes()
            .to_vec(),
        )
    }

    fn key_names() -> Vec<String> {
        vec!["DbTimestamp".to_string()]
    }
}

/// Uniquely identifies a record in the database.
pub type DataIdentifier = u64;
pub type DataExpiration = DateTime<Utc>;

/// Maintains a real-time cache of persistent objects in the database of
/// type `T`.
#[derive(Clone)]
pub struct Resident<T>
where
    T: Data,
{
    db: Arc<native_db::Database<'static>>,
    inner: Arc<RwLock<T>>,

    watch: Option<(u64, CancellationToken)>,
}

impl<T: Data> Drop for Resident<T> {
    fn drop(&mut self) {
        if let Some((watch_id, token)) = self.watch.as_ref() {
            token.cancel();
            self.db.unwatch(*watch_id).unwrap();
        }
    }
}

impl<T: Data> Resident<T> {
    pub fn detached(value: T) -> Result<Self> {
        todo!()
    }
}

impl<T: Data + 'static> Resident<T> {
    /// Create a new `Watch` when there's only one row in the database.
    pub fn singleton(db: Arc<native_db::Database<'static>>) -> Result<Self> {
        let r = db.r_transaction()?;
        let mut rows: Vec<T> = r.scan().primary()?.all()?.try_collect()?;

        let item = if rows.len() > 1 {
            bail!("Too many rows");
        } else if rows.len() == 1 {
            let first = rows.pop().unwrap();

            // Populate empty table
            let rw = db.rw_transaction()?;
            rw.insert(first.clone())?;
            rw.commit()?;
            first
        } else {
            T::default()
        };

        let (mut channel, watch_id) = db.watch().get().primary::<T>(item.id())?;

        let cancel_token = CancellationToken::new();
        let token = cancel_token.clone();

        let cache = Arc::new(RwLock::new(item));

        tokio::spawn({
            let cache_clone = Arc::clone(&cache);
            async move {
                loop {
                    tokio::select! {
                        _ = token.cancelled() => {
                            break;
                        }
                        event = channel.recv() => match event {
                            Some(event) => match event {
                                Event::Insert(data) => {}
                                Event::Update(data) => match data.inner_new() {
                                    Ok(d) => {
                                        let mut c = cache_clone.write().await;
                                        *c = d;
                                    },
                                    Err(_) => {},
                                }
                                Event::Delete(data) => {}
                            }
                            None => {
                                break;
                            }
                        }
                    }
                }
            }
        });

        Ok(Self {
            inner: cache,
            db,
            watch: Some((watch_id, cancel_token)),
        })
    }

    pub async fn read(&self) -> RwLockReadGuard<'_, T> {
        self.inner.read().await
    }

    pub async fn write(&self) -> RwLockWriteGuard<'_, T> {
        self.inner.write().await
    }
}

impl<T: Data> Resident<T> {
    pub async fn update<F>(&self, mutator: F) -> Result<()>
    where
        F: Fn(&mut T) -> Result<()>,
    {
        let cache = self.inner.read().await;
        let mut next = cache.clone();
        mutator(&mut next)?;

        if next != *cache {
            let rw = self.db.rw_transaction()?;
            rw.upsert(next.clone())?;
            rw.commit()?;

            drop(cache);

            let mut cache = self.inner.write().await;
            *cache = next;
        }

        Ok(())
    }
}

impl<T: HistoricalData> Resident<T> {
    pub async fn history(&self, range: RangeFrom<DbTimestamp>) -> Result<Vec<T>> {
        // Get values of all secondary keys
        let mut secondary_keys = (*self.inner.read().await).native_db_secondary_keys();

        // Remove timestamp because we're going to set that one manually
        secondary_keys.retain(|key_def, _| *key_def != T::timestamp_key());

        let r = self.db.r_transaction()?;

        // TODO use the most restrictive condition first for performance
        // let mut it = r.scan().secondary(T::timestamp_key())?.range(range)?;

        // for (key_def, key) in secondary_keys.into_iter() {
        //     it = it.and(r.scan().secondary(key_def)?.equal(match key {
        //         KeyEntry::Default(key) => key,
        //         KeyEntry::Optional(key) => key.unwrap(), // TODO
        //     })?);
        // }

        // Ok(it.try_collect()?)
        todo!()
    }
}

#[cfg(test)]
mod test_resident {
    use super::*;
    use anyhow::Result;
    use native_db::Models;
    use native_db::*;
    use native_model::{Model, native_model};
    use sandpolis_macros::{Data, HistoricalData, data};
    use serde::{Deserialize, Serialize};
    use tokio::time::{Duration, sleep};

    #[data]
    pub struct TestData {
        #[secondary_key]
        pub a: String,
        #[secondary_key]
        pub b: String,
    }

    #[data(history)]
    pub struct TestHistoryData {
        #[secondary_key]
        pub a: String,
        pub b: String,
    }

    #[tokio::test]
    async fn test_build_database() -> Result<()> {
        let models = Box::leak(Box::new(Models::new()));
        models.define::<TestData>().unwrap();

        let mut database = DatabaseLayer::new(
            DatabaseConfig {
                storage: None,
                ephemeral: true,
            },
            models,
        )?;
        database.add_realm("default".parse()?).await?;

        let db = database.get(Some("default".parse()?)).await?;
        let watch: Resident<TestData> = Resident::singleton(db.clone())?;

        // Update data a bunch of times
        for i in 1..10 {
            watch
                .update(|data| {
                    data.a = format!("test {i}");
                    Ok(())
                })
                .await?;
        }

        // Database should reflect "test 9"
        {
            let r = db.r_transaction()?;
            let items: Vec<TestData> = r.scan().primary()?.all()?.try_collect()?;
            assert_eq!(items.len(), 1);
            assert_eq!(items[0].a, "test 9");
        }

        // Change it in the database
        {
            let rw = db.rw_transaction()?;
            rw.upsert(TestData {
                _id: watch.read().await._id,
                a: "test 10".into(),
                b: "".into(),
            })?;
            rw.commit()?;
        }

        // Watch should reflect "test 10" after a while
        sleep(Duration::from_secs(1)).await;
        assert_eq!(watch.read().await.a, "test 10");

        Ok(())
    }

    #[tokio::test]
    async fn test_historical_data() -> Result<()> {
        let models = Box::leak(Box::new(Models::new()));
        models.define::<TestHistoryData>().unwrap();

        let mut database = DatabaseLayer::new(
            DatabaseConfig {
                storage: None,
                ephemeral: true,
            },
            models,
        )?;

        let db = database.get(None).await?;
        let watch: Resident<TestHistoryData> = Resident::singleton(db.clone())?;

        // Update data a bunch of times
        for i in 1..10 {
            watch
                .update(|data| {
                    data.b = format!("test {i}");
                    Ok(())
                })
                .await?;
        }

        // Database should have 10 items
        {
            let r = db.r_transaction()?;
            let items: Vec<TestHistoryData> = r.scan().primary()?.all()?.try_collect()?;
            assert_eq!(items.len(), 10);
        }

        // Check history
        {
            assert_eq!(
                watch
                    .history(DbTimestamp::Latest(DateTime::default())..)
                    .await?
                    .len(),
                0
            );
        }

        Ok(())
    }
}

#[derive(Clone)]
pub struct ResidentVec<T>
where
    T: Data,
{
    db: Arc<native_db::Database<'static>>,
    inner: Arc<RwLock<Vec<Resident<T>>>>,

    watch: Option<(u64, CancellationToken)>,
}

impl<T: Data> Drop for ResidentVec<T> {
    fn drop(&mut self) {
        if let Some((watch_id, token)) = self.watch.as_ref() {
            token.cancel();
            self.db.unwatch(*watch_id).unwrap();
        }
    }
}

impl<T: Data> ResidentVec<T> {
    pub fn is_detached(&self) -> bool {
        self.watch.is_none()
    }

    /// Create a new detached `ResidentVec` which will not receive any updates
    /// after its created.
    fn detached(db: Arc<native_db::Database<'static>>, conditions: Vec<Condition>) -> Result<Self> {
        let r = db.r_transaction()?;
        let scan = r.scan();

        // We have to store these temporarily until we collect()
        let mut scans = Vec::new();

        Ok(Self {
            inner: Arc::new(RwLock::new(
                conditions
                    .into_iter()
                    .map(|condition| match condition {
                        Condition::Equal { key, value } => {
                            let scan = scan.secondary(key).unwrap();
                            let it = {
                                let it_ref = scan.equal(value).unwrap();
                                it_ref
                            };
                            scans.push(scan);
                            it
                        }
                        Condition::Range { key, value } => {
                            todo!()
                        }
                    })
                    .reduce(|x, y| x.and(y))
                    .unwrap()
                    .map(|item| item.map(|i| Resident::detached(i).unwrap()))
                    .try_collect()?,
            )),
            watch: None,
            db,
        })
    }
}

// Replicate some of the Vec API
impl<T: Data> ResidentVec<T> {
    /// Returns the number of elements in the vector, also referred to
    /// as its 'length'.
    pub async fn len(&self) -> usize {
        self.inner.read().await.len()
    }

    /// Appends an element to the back of a collection.
    pub async fn push(&self, value: T) -> Result<()> {
        // TODO check id collision?
        if self.watch.is_some() {
            // Insert and let watcher update `inner`
            let rw = self.db.rw_transaction()?;
            rw.insert(value)?;
            rw.commit()?;

            // TODO wait for update?
        } else {
            // Detached, so simple append
            self.inner.write().await.push(Resident::detached(value)?);
        }

        Ok(())
    }
}

enum Condition {
    Equal {
        key: KeyDefinition<KeyOptions>,
        value: Key,
    },
    Range {
        key: KeyDefinition<KeyOptions>,
        value: KeyRange,
    },
}

pub struct ResidentVecBuilder<T>
where
    T: Data,
{
    db: Arc<native_db::Database<'static>>,
    _phantom: PhantomData<T>,
    conditions: Vec<Condition>,
}

impl<T: Data> ResidentVecBuilder<T> {
    pub fn equal(mut self, key: impl ToKeyDefinition<KeyOptions>, value: impl ToKey) -> Self {
        self.conditions.push(Condition::Equal {
            key: key.key_definition(),
            value: value.to_key(),
        });
        self
    }
    pub fn range<R: RangeBounds<impl ToKey>>(
        mut self,
        key: impl ToKeyDefinition<KeyOptions>,
        value: R,
    ) -> Self {
        self.conditions.push(Condition::Range {
            key: key.key_definition(),
            value: KeyRange::new(value),
        });
        self
    }

    pub fn current(mut self) -> Result<ResidentVec<T>> {
        ResidentVec::detached(self.db, self.conditions)
    }
}

impl<T: HistoricalData> ResidentVecBuilder<T> {
    pub fn latest(mut self) {}
    pub fn previous(mut self) {}
}
