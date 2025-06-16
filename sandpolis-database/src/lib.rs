#![feature(iterator_try_collect)]
#![feature(lock_value_accessors)]

use crate::config::DatabaseConfig;
use anyhow::{Result, anyhow, bail};
use chrono::{DateTime, Utc};
use native_db::db_type::{KeyDefinition, KeyEntry, KeyOptions, ToKeyDefinition};
use native_db::transaction::RTransaction;
use native_db::transaction::query::SecondaryScanIterator;
use native_db::watch::Event;
use native_db::{Key, Models, ToInput, ToKey};
use sandpolis_core::{InstanceId, RealmName};
use serde::{Deserialize, Serialize};
use std::collections::{BTreeMap, HashMap};
use std::ops::{Range, RangeFrom};
use std::path::Path;
use std::sync::atomic::AtomicU64;
use std::{marker::PhantomData, sync::Arc};
use tokio::sync::{RwLock, RwLockReadGuard};
use tokio_util::sync::CancellationToken;
use tracing::instrument::WithSubscriber;
use tracing::{debug, trace};

pub mod config;

#[derive(Clone)]
pub struct DatabaseLayer {
    config: DatabaseConfig,
    models: &'static Models,
    inner: Arc<RwLock<HashMap<Option<RealmName>, Arc<native_db::Database<'static>>>>>,
}

impl DatabaseLayer {
    pub fn new(config: DatabaseConfig, models: &'static Models) -> Result<Self> {
        let system = if let Some(path) = config.get_storage_dir()? {
            let path = path.join("system.db");
            debug!(path = %path.display(), "Initializing persistent system database");

            Arc::new(native_db::Builder::new().create(models, path)?)
        } else {
            debug!("Initializing ephemeral system database");
            Arc::new(native_db::Builder::new().create_in_memory(models)?)
        };

        Ok(Self {
            config,
            models,
            inner: Arc::new(RwLock::new(HashMap::from([(None, system)]))),
        })
    }

    /// Load or create a new database for the given realm.
    pub async fn add_realm(
        &mut self,
        name: RealmName,
    ) -> Result<Arc<native_db::Database<'static>>> {
        // Check for duplicates
        let mut inner = self.inner.write().await;
        if inner.contains_key(&Some(name.clone())) {
            bail!("Duplicate realm");
        }

        let db = if let Some(path) = self.config.get_storage_dir()? {
            let path = path.join(format!("{name}.db"));

            debug!(realm = %name, path = %path.display(), "Initializing persistent realm database");
            Arc::new(native_db::Builder::new().create(self.models, path)?)
        } else {
            debug!(realm = %name, "Initializing ephemeral realm database");
            Arc::new(native_db::Builder::new().create_in_memory(self.models)?)
        };
        inner.insert(Some(name), db.clone());

        Ok(db)
    }

    pub async fn get(&self, name: Option<RealmName>) -> Result<Arc<native_db::Database<'static>>> {
        let inner = self.inner.read().await;
        if let Some(db) = inner.get(&name) {
            return Ok(db.clone());
        }
        bail!("Realm not found");
    }
}

// TODO if we define update, etc on these traits and pass in the db as a
// parameter, we can use dynamic dispatch to handle different impl for versioned
// Data instead of separate helper struct

/// `Data` is what's stored in a database!
pub trait Data
where
    Self: ToInput + Clone + PartialEq + Send + Sync,
{
    fn id(&self) -> DataIdentifier;
    fn set_id(&mut self, id: DataIdentifier);
}

/// `Data` that maintains a history of its value over time.
pub trait HistoricalData
where
    Self: Data,
{
    fn timestamp(&self) -> DbTimestamp;
    fn set_timestamp(&self, timestamp: DbTimestamp);

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
    cache: Arc<RwLock<T>>,

    /// Used to stop the database from sending updates
    watch_id: u64,

    /// Allows the background update thread to be stopped
    cancel_token: CancellationToken,
}

impl<T: Data> Drop for Resident<T> {
    fn drop(&mut self) {
        self.cancel_token.cancel();
        self.db.unwatch(self.watch_id).unwrap();
    }
}

impl<T: Data + Default + 'static> Resident<T> {
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
            cache,
            watch_id,
            cancel_token,
            db,
        })
    }

    pub async fn read(&self) -> RwLockReadGuard<'_, T> {
        self.cache.read().await
    }
}

impl<T: Data> Resident<T> {
    pub async fn update<F>(&self, mutator: F) -> Result<()>
    where
        F: Fn(&mut T) -> Result<()>,
    {
        let cache = self.cache.read().await;
        let mut next = cache.clone();
        mutator(&mut next)?;

        if next != *cache {
            let rw = self.db.rw_transaction()?;
            rw.upsert(next.clone())?;
            rw.commit()?;

            drop(cache);

            let mut cache = self.cache.write().await;
            *cache = next;
        }

        Ok(())
    }
}

impl<T: HistoricalData> Resident<T> {
    pub async fn history(&self, range: RangeFrom<DbTimestamp>) -> Result<Vec<T>> {
        // Get values of all secondary keys
        let mut secondary_keys = (*self.cache.read().await).native_db_secondary_keys();

        // Remove timestamp because we're going to set that one manually
        secondary_keys.retain(|key_def, _| *key_def != T::timestamp_key());

        let r = self.db.r_transaction()?;

        // TODO use the most restrictive condition first for performance
        let mut it = r.scan().secondary(T::timestamp_key())?.range(range)?;

        for (key_def, key) in secondary_keys.into_iter() {
            it = it.and(r.scan().secondary(key_def)?.equal(match key {
                KeyEntry::Default(key) => key,
                KeyEntry::Optional(key) => key.unwrap(), // TODO
            })?);
        }

        Ok(it.try_collect()?)
    }
}

#[cfg(test)]
mod test_resident {
    use super::*;
    use anyhow::Result;
    use native_db::Models;
    use native_db::*;
    use native_model::{Model, native_model};
    use sandpolis_macros::{Data, HistoricalData};
    use serde::{Deserialize, Serialize};
    use tokio::time::{Duration, sleep};

    #[derive(Serialize, Deserialize, PartialEq, Debug, Clone, Default, Data)]
    #[native_model(id = 5, version = 1)]
    #[native_db]
    pub struct TestData {
        #[primary_key]
        pub _id: DataIdentifier,

        #[secondary_key]
        pub a: String,
        #[secondary_key]
        pub b: String,
    }

    #[derive(Serialize, Deserialize, PartialEq, Debug, Clone, Default, Data, HistoricalData)]
    #[native_model(id = 6, version = 1)]
    #[native_db]
    pub struct TestHistoryData {
        #[primary_key]
        pub _id: DataIdentifier,

        #[secondary_key]
        pub _timestamp: DbTimestamp,

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
    cache: Arc<RwLock<Vec<Resident<T>>>>,

    /// Used to stop the database from sending updates
    watch_id: u64,

    /// Allows the background update thread to be stopped
    cancel_token: CancellationToken,
}

impl<T: Data> Drop for ResidentVec<T> {
    fn drop(&mut self) {
        self.cancel_token.cancel();
        self.db.unwatch(self.watch_id).unwrap();
    }
}

impl<T: Data> ResidentVec<T> {
    pub fn is_detached(&self) -> bool {
        false
    }
}
