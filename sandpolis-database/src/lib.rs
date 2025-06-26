#![feature(iterator_try_collect)]
#![feature(lock_value_accessors)]

use crate::config::DatabaseConfig;
use anyhow::{Result, anyhow, bail};
use chrono::{DateTime, Utc};
use native_db::db_type::{KeyDefinition, KeyEntry, KeyOptions, KeyRange, ToKeyDefinition};
use native_db::transaction::query::{SecondaryScan, SecondaryScanIterator};
use native_db::transaction::{RTransaction, RwTransaction};
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
use tracing::{debug, trace, warn};

pub mod config;

/// This layer manages separate databases for each realm.
#[derive(Clone)]
pub struct DatabaseLayer {
    config: DatabaseConfig,
    models: &'static Models,
    inner: Arc<RwLock<BTreeMap<RealmName, RealmDatabase>>>,
}

impl DatabaseLayer {
    /// Create a new `DatabaseLayer` initialized with the default realm.
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
            inner: Arc::new(RwLock::new(BTreeMap::from([(
                RealmName::default(),
                RealmDatabase::new(default),
            )]))),
        })
    }

    /// Load an existing or create a new `RealmDatabase` for the given realm.
    pub async fn realm(&mut self, name: RealmName) -> Result<RealmDatabase> {
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
            RealmDatabase::new(native_db::Builder::new().create(self.models, path)?)
        } else {
            debug!(realm = %name, "Initializing ephemeral realm database");
            RealmDatabase::new(native_db::Builder::new().create_in_memory(self.models)?)
        };
        inner.insert(name, db.clone());

        Ok(db)
    }
}

#[macro_export]
macro_rules! query {
    // Match the pattern where conditions are provided in a comma-separated list
    ($($condition:expr),*) => {{
        // Return a closure that takes a Vec<Item> and returns a filtered Vec<Item>
        move |items: Vec<Item>| {
            items.into_iter()
                .filter(|item| {
                    // Check if all the conditions hold for the item
                    $(
                        $condition
                    &&)* true // Ensures conditions are AND-ed together
                })
                .collect::<Vec<Item>>() // Collect the filtered items into a Vec
        }
    }};
}

/// Database handle containing all `Data` for a particular realm.
#[derive(Clone)]
pub struct RealmDatabase(Arc<native_db::Database<'static>>);

impl RealmDatabase {
    fn new(inner: native_db::Database<'static>) -> Self {
        Self(Arc::new(inner))
    }

    /// Direct access to a new read-write transaction.
    pub fn rw_transaction(&self) -> Result<RwTransaction> {
        Ok(self.0.rw_transaction()?)
    }

    /// Direct access to a new read-only transaction.
    pub fn r_transaction(&self) -> Result<RTransaction> {
        Ok(self.0.r_transaction()?)
    }

    pub fn attach_one<T: Data>(&self, query: Q) -> Result<Resident<T>>
    where
        Q: Fn(RTransaction) -> Vec<T>,
    {
        todo!()
    }

    pub fn attach<T: Data>(&self, query: Q) -> Result<ResidentVec<T>>
    where
        Q: Fn(RTransaction) -> Vec<T>,
    {
        todo!()
    }

    /// Get a `Resident` for a type that only exists once.
    pub fn singleton<T: Data>(&self) -> Result<Resident<T>> {
        let r = self.0.r_transaction()?;
        let mut rows: Vec<T> = r.scan().primary()?.all()?.try_collect()?;

        let item = if rows.len() > 1 {
            bail!("Too many rows");
        } else if rows.len() == 1 {
            let first = rows.pop().unwrap();

            // Populate empty table
            let rw = self.0.rw_transaction()?;
            rw.insert(first.clone())?;
            rw.commit()?;
            first
        } else {
            T::default()
        };

        let (mut channel, watch_id) = self.0.watch().get().primary::<T>(item.id())?;

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
                                Event::Delete(_) => warn!("Deleting a singleton is undefined"),
                            }
                            None => {
                                break;
                            }
                        }
                    }
                }
            }
        });

        Ok(Resident {
            inner: cache,
            db: self.0.clone(),
            watch: Some((watch_id, cancel_token)),
        })
    }

    pub fn query<T: Data>(&self) -> ResidentVecBuilder<T> {
        ResidentVecBuilder {
            db: self.0.clone(),
            _phantom: PhantomData,
            conditions: Vec::new(),
        }
    }
}

/// `Data` are rows in a database.
pub trait Data
where
    Self: ToInput + Clone + PartialEq + Send + Sync + Default,
{
    /// Get the unique identifier for this particular `Data`.
    fn id(&self) -> DataIdentifier;

    fn generation(&self) -> DataGeneration;

    fn generaion_key() -> KeyDefinition<KeyOptions> {
        KeyDefinition::new(
            Self::native_model_id(),
            Self::native_model_version(),
            "_generation",
            <String>::key_names(),
            KeyOptions {
                unique: false,
                optional: false,
            },
        )
    }

    /// If an expiration timestamp is given, previous generations of the `Data`
    /// will be kept until the expiration date. Otherwise, only the latest
    /// generation is kept.
    fn expiration(&self) -> Option<DataExpiration>;
}

// TODO Eq based only on inner?

/// Generations are previous versions of `Data`.
#[derive(Eq, PartialEq, Debug, Clone, Copy, Hash)]
pub enum DataGeneration {
    Latest(DateTime<Utc>),
    Previous(DateTime<Utc>),
}

impl Serialize for DataGeneration {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        serializer.serialize_i64(match self {
            DataGeneration::Latest(dt) => dt.timestamp_millis(),
            // Previous values are stored as negative so we can tell the difference
            DataGeneration::Previous(dt) => -dt.timestamp_millis(),
        })
    }
}

struct DataGenerationVisitor;

impl<'de> Visitor<'de> for DataGenerationVisitor {
    type Value = DataGeneration;

    fn expecting(&self, formatter: &mut fmt::Formatter) -> fmt::Result {
        formatter.write_str("an i64")
    }

    fn visit_i64<E>(self, value: i64) -> Result<Self::Value, E>
    where
        E: de::Error,
    {
        if value < 0 {
            Ok(DataGeneration::Previous(
                DateTime::from_timestamp_millis(-value).ok_or(anyhow!("Timestamp out of range"))?,
            ))
        } else {
            Ok(DataGeneration::Latest(
                DateTime::from_timestamp_millis(value).ok_or(anyhow!("Timestamp out of range"))?,
            ))
        }
    }
}

impl<'de> Deserialize<'de> for DataGeneration {
    fn deserialize<D>(deserializer: D) -> Result<DataGeneration, D::Error>
    where
        D: Deserializer<'de>,
    {
        deserializer.deserialize_i64(DataGenerationVisitor)
    }
}

impl Default for DataGeneration {
    fn default() -> Self {
        Self::Latest(DateTime::default())
    }
}

impl ToKey for DataGeneration {
    fn to_key(&self) -> native_db::Key {
        native_db::Key::new(
            match self {
                DataGeneration::Latest(dt) => dt.timestamp_millis(),
                // Previous values are stored as negative so we can tell the difference
                DataGeneration::Previous(dt) => -dt.timestamp_millis(),
            }
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

    // TODO dont allow detach?
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

impl<T: Data + 'static> Resident<T> {
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

impl<T: TemporalData> Resident<T> {
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
    use crate as sandpolis_database;
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

        let db = database.realm(RealmName::default()).await?;
        let watch: Resident<TestData> = db.singleton()?;

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

        let db = database.realm(RealmName::default()).await?;
        let watch: Resident<TestHistoryData> = db.singleton()?;

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

        Ok(Self {
            inner: Arc::new(RwLock::new(
                conditions
                    .into_iter()
                    .map(|condition| match condition {
                        Condition::Equal { key, value } => {
                            // TODO leaked temporary value!!!!!!!!
                            let scan = Box::leak(Box::new(scan.secondary(key).unwrap()));
                            scan.equal(value).unwrap()
                        }
                        Condition::Range { key, value } => {
                            // TODO leaked temporary value!!!!!!!!
                            let scan = Box::leak(Box::new(scan.secondary(key).unwrap()));
                            scan.range(value).unwrap()
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
    pub async fn push(&self, value: T) -> Result<&Resident<T>> {
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

        Ok(todo!())
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

impl<T: TemporalData> ResidentVecBuilder<T> {
    pub fn latest(mut self) {}
    pub fn previous(mut self) {}
}

#[cfg(test)]
mod test_resident_vec {
    use super::*;
    use crate as sandpolis_database;
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

    #[tokio::test]
    async fn test_nonhistorical() -> Result<()> {
        let models = Box::leak(Box::new(Models::new()));
        models.define::<TestData>().unwrap();

        let mut database = DatabaseLayer::new(
            DatabaseConfig {
                storage: None,
                ephemeral: true,
            },
            models,
        )?;

        let db = database.realm(RealmName::default()).await?;
        let test_data: ResidentVec<TestData> = db.query().equal(TestDataKey::a, "A").current()?;

        assert_eq!(test_data.len().await, 0);

        // Add item
        let data: &Resident<TestData> = test_data
            .push(TestData {
                a: "A".to_string(),
                b: "B".to_string(),
                ..Default::default()
            })
            .await?;

        assert_eq!(test_data.len().await, 1);

        // Update a bunch of times
        for i in 1..10 {
            data.update(|d| {
                d.a = format!("test {i}");
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
                _id: data.read().await._id,
                a: "test 10".into(),
                b: "".into(),
            })?;
            rw.commit()?;
        }

        // Should reflect "test 10" after a while
        sleep(Duration::from_secs(1)).await;
        assert_eq!(data.read().await.a, "test 10");

        Ok(())
    }
}
