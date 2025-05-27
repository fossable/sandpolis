use anyhow::{Result, anyhow, bail};
use chrono::{DateTime, Utc};
use native_db::{Models, ToInput, ToKey};
use sandpolis_core::{GroupName, InstanceId};
use serde::{Deserialize, Serialize};
use std::collections::BTreeMap;
use std::ops::Range;
use std::path::Path;
use std::{marker::PhantomData, sync::Arc};
use tracing::{debug, trace};

pub mod config;

pub struct DatabaseBuilder {
    models: &'static Models,
    groups: Vec<GroupDatabase>,
}

impl DatabaseBuilder {
    pub fn new(models: &'static Models) -> Self {
        Self {
            models,
            // Don't bother with a map because groups are few
            groups: Vec::with_capacity(1),
        }
    }

    /// Create a new ephemeral database for the given group name.
    pub fn add_ephemeral(mut self, name: GroupName) -> Result<Self> {
        // Check for duplicates
        for group in self.groups.iter() {
            if name == group.name {
                bail!("Duplicate group");
            }
        }

        debug!(group = %name, "Initializing ephemeral database");
        self.groups.push(GroupDatabase {
            name,
            db: Arc::new(native_db::Builder::new().create_in_memory(self.models)?),
        });

        Ok(self)
    }

    /// Load or create a new persistent database for the given group name.
    pub fn add_persistent<P>(mut self, name: GroupName, path: P) -> Result<Self>
    where
        P: AsRef<Path>,
    {
        let path = path.as_ref();

        // Check for duplicates
        for group in self.groups.iter() {
            if name == group.name {
                bail!("Duplicate group");
            }
        }

        debug!(group = %name, path = %path.display(), "Initializing persistent database");
        self.groups.push(GroupDatabase {
            name,
            db: Arc::new(native_db::Builder::new().create(self.models, path)?),
        });

        Ok(self)
    }

    pub fn build(self) -> Database {
        Database(Arc::new(self.groups))
    }
}

#[derive(Clone)]
pub struct Database(Arc<Vec<GroupDatabase>>);

impl Database {
    pub fn get<'a>(&'a self, name: GroupName) -> Result<&'a GroupDatabase> {
        let name = name.to_string();
        for db in self.0.iter() {
            if name == *db.name {
                return Ok(db);
            }
        }
        bail!("Group not found");
    }
}

pub trait Data
where
    Self: ToInput,
{
    fn id(&self) -> DataIdentifier;
}

pub trait HistoricalData
where
    Self: Data,
{
    fn timestamp(&self) -> DbTimestamp;
}

pub trait ExpiringData
where
    Self: Data,
{
    fn expiration(&self) -> DataExpiration;
}

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
    use sandpolis_macros::Data;
    use serde::{Deserialize, Serialize};

    #[derive(Serialize, Deserialize, PartialEq, Debug, Data)]
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

        Ok(())
    }
}

/// Each group has a separate database for isolation.
#[derive(Clone)]
pub struct GroupDatabase {
    name: GroupName,
    db: Arc<native_db::Database<'static>>,
}

impl GroupDatabase {
    pub fn view<T>(&self) -> DataView<T>
    where
        T: Data,
    {
        DataView::new(self.db.clone()).unwrap()
    }

    pub fn instance_view<T>(id: InstanceId) -> DataView<T>
    where
        T: ToInput,
    {
        todo!()
    }
}

// TODO Eq based only on inner?
#[derive(Serialize, Deserialize, Eq, PartialEq, Debug, Clone, Hash)]
pub enum DbTimestamp {
    Latest(DateTime<Utc>),
    Previous(DateTime<Utc>),
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
    T: ToInput,
{
    db: Arc<native_db::Database<'static>>,
    cache: Arc<BTreeMap<DataIdentifier, T>>,
    watch_id: u64,
    next_id: DataIdentifier,
}

impl<T: Data> DataView<T> {
    pub fn new(db: Arc<native_db::Database<'static>>) -> Result<Self> {
        let (channel, watch_id) = db.watch().get().primary::<T>(1)?;
        Ok(Self {
            db,
            cache: Arc::new(BTreeMap::new()),
            watch_id,
            next_id: 1,
        })
    }
}

impl<T: Data + Default> DataView<T> {
    pub fn initialize<I>(&self, initializer: I)
    where
        I: Fn(&mut T),
    {
        let mut row = T::default();
        initializer(&mut row);

        let rw = self.db.rw_transaction().unwrap();
        rw.insert(row);
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
