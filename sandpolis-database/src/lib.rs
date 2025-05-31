#![feature(iterator_try_collect)]

use crate::config::DatabaseConfig;
use anyhow::{Result, anyhow, bail};
use chrono::{DateTime, Utc};
use native_db::db_type::{KeyDefinition, KeyOptions, ToKeyDefinition};
use native_db::transaction::RTransaction;
use native_db::transaction::query::SecondaryScanIterator;
use native_db::{Key, Models, ToInput, ToKey};
use redb::ReadOnlyTable;
use sandpolis_core::{GroupName, InstanceId};
use serde::{Deserialize, Serialize};
use std::collections::{BTreeMap, HashMap};
use std::ops::Range;
use std::path::Path;
use std::sync::RwLock;
use std::sync::atomic::AtomicU64;
use std::{marker::PhantomData, sync::Arc};
use tracing::instrument::WithSubscriber;
use tracing::{debug, trace};

pub mod config;

#[derive(Clone)]
pub struct DatabaseLayer {
    config: DatabaseConfig,
    models: &'static Models,
    inner: Arc<RwLock<HashMap<Option<GroupName>, Arc<native_db::Database<'static>>>>>,
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

    /// Load or create a new database for the given group.
    pub fn add_group(&mut self, name: GroupName) -> Result<()> {
        // Check for duplicates
        let mut inner = self.inner.write().unwrap();
        if inner.contains_key(&Some(name.clone())) {
            bail!("Duplicate group");
        }

        if let Some(path) = self.config.get_storage_dir()? {
            let path = path.join(format!("{name}.db"));
            debug!(group = %name, path = %path.display(), "Initializing persistent group database");

            inner.insert(
                Some(name),
                Arc::new(native_db::Builder::new().create(self.models, path)?),
            );
        } else {
            debug!(group = %name, "Initializing ephemeral group database");
            inner.insert(
                Some(name),
                Arc::new(native_db::Builder::new().create_in_memory(self.models)?),
            );
        }

        Ok(())
    }

    pub fn get(&self, name: Option<GroupName>) -> Result<Arc<native_db::Database<'static>>> {
        let inner = self.inner.read().unwrap();
        if let Some(db) = inner.get(&name) {
            return Ok(db.clone());
        }
        bail!("Group not found");
    }
}

/// `Data` is what's stored in a database!
pub trait Data
where
    Self: ToInput + Default + Clone,
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

/// `Data` that expires and will be removed from the database.
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

#[cfg(test)]
mod test_database {
    use super::*;
    use anyhow::Result;
    use native_db::Models;
    use native_db::*;
    use native_model::{Model, native_model};
    use sandpolis_macros::{Data, HistoricalData};
    use serde::{Deserialize, Serialize};

    #[derive(Serialize, Deserialize, PartialEq, Debug, Clone, Default, Data)]
    #[native_model(id = 5, version = 1)]
    #[native_db]
    pub struct TestData {
        #[primary_key]
        pub _id: DataIdentifier,

        #[secondary_key]
        pub _timestamp: DbTimestamp,
        #[secondary_key]
        pub a: String,
        #[secondary_key]
        pub b: String,
    }

    #[test]
    fn test_build_database() -> Result<()> {
        let models = Box::leak(Box::new(Models::new()));
        models.define::<TestData>().unwrap();

        let databases = DatabaseBuilder::new(models)
            .add_ephemeral("default".parse()?)?
            .build();

        let db = databases.get("default".parse()?)?;
        let view: DataView<TestData> = db.view();

        for i in 1..10 {
            view.initialize(|item| {
                item.a = format!("test {i}");
            })?;
        }

        Ok(())
    }
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
pub struct DataView<T>
where
    T: Data,
{
    db: Arc<native_db::Database<'static>>,
    cache: Arc<RwLock<BTreeMap<DataIdentifier, T>>>,
    watch_id: u64,
    next_id: Arc<AtomicU64>,
}

impl<T: Data> DataView<T> {
    pub fn new(db: Arc<native_db::Database<'static>>) -> Result<Self> {
        let (channel, watch_id) = db.watch().scan().primary().all::<T>()?;

        Ok(Self {
            cache: Arc::new(RwLock::new(BTreeMap::new())),
            watch_id,
            next_id: Arc::new(AtomicU64::new(
                // TODO correct table
                db.redb_stats()?.primary_tables[0].n_entries.unwrap(),
            )),
            db,
        })
    }
}

impl<T: Data> DataView<T> {
    pub fn initialize<I>(&self, initializer: I) -> Result<()>
    where
        I: Fn(&mut T),
    {
        let mut row = T::default();
        row.set_id(
            self.next_id
                .fetch_add(1, std::sync::atomic::Ordering::Relaxed),
        );
        initializer(&mut row);

        let rw = self.db.rw_transaction().unwrap();
        rw.insert(row.clone())?;
        self.cache.write().unwrap().insert(row.id(), row);
        Ok(())
    }

    // TODO use a cell instead of mut self
    pub fn update<F>(&mut self, id: DataIdentifier, mutator: F) -> Result<()>
    where
        F: Fn(&mut T) -> Result<()>,
    {
        // let mut new_value = self.value.clone();
        // mutator(&mut new_value)?;
        // let rw = self.db.rw_transaction()?;
        // rw.upsert(new_value.clone())?;
        // rw.commit()?;

        // self.value = new_value;
        Ok(())
    }
}

impl<T: HistoricalData> DataView<T> {
    pub fn history(&self, id: DataIdentifier, range: Range<DbTimestamp>) -> Result<Vec<T>> {
        let r = self.db.r_transaction()?;
        Ok(r.scan()
            .secondary(T::timestamp_key())?
            .range(range)?
            .try_collect()?)
    }
}
