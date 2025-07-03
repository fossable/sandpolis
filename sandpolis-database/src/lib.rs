#![feature(iterator_try_collect)]
#![feature(lock_value_accessors)]

use crate::config::DatabaseConfig;
use anyhow::{Result, anyhow, bail};
use chrono::{DateTime, Utc};
use native_db::db_type::{KeyDefinition, KeyEntry, KeyOptions, KeyRange, ToKeyDefinition};
use native_db::transaction::query::{RScan, SecondaryScan, SecondaryScanIterator};
use native_db::transaction::{RTransaction, RwTransaction};
use native_db::watch::Event;
use native_db::{Key, Models, ToInput, ToKey};
use sandpolis_core::{InstanceId, RealmName};
use serde::de::Visitor;
use serde::{Deserialize, Deserializer, Serialize, Serializer};
use std::collections::{BTreeMap, HashMap};
use std::ops::{Add, Range, RangeBounds, RangeFrom};
use std::path::Path;
use std::sync::atomic::AtomicU64;
use std::{marker::PhantomData, sync::Arc};
use tokio::sync::{RwLock, RwLockReadGuard, RwLockWriteGuard};
use tokio_util::sync::CancellationToken;
use tracing::instrument::WithSubscriber;
use tracing::{debug, trace, warn};
use validator::ValidateRequired;

pub mod config;

/// This layer manages separate databases for each realm.
#[derive(Clone)]
pub struct DatabaseLayer {
    config: DatabaseConfig,

    /// Container for all the possible types of `Data` we can interact with
    models: &'static Models,

    /// Separate databases indexed by name
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
    pub async fn realm(&self, name: RealmName) -> Result<RealmDatabase> {
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

    pub fn resident<T: Data + Default + 'static>(
        &self,
        query: impl DataQuery<T>,
    ) -> Result<Resident<T>> {
        // Hold onto this transaction until we've created the watch channel so
        // we don't miss any updates.
        let r = self.0.r_transaction()?;
        let mut items = query.query(r.scan())?;

        let item = if items.len() > 1 {
            bail!("Too many items");
        } else if items.len() == 1 {
            items.pop().unwrap()
        } else {
            let default = T::default();
            let rw = self.0.rw_transaction()?;
            rw.insert(default.clone())?;
            rw.commit()?;
            default
        };

        // Setup watcher so we get updates
        let (mut channel, watch_id) = if item.expiration().is_none() {
            // Watch the primary ID if this isn't temporal data because it can't change
            self.0.watch().get().primary::<T>(item.id())?
        } else {
            // Otherwise, watch the secondary ID
            todo!()
        };

        // Safe to end the transaction once the watcher is registered
        drop(r);

        let token = CancellationToken::new();
        let resident = Resident {
            inner: Arc::new(RwLock::new(item)),
            db: self.0.clone(),
            watch: Some((watch_id, token.clone())),
        };
        let resident_clone = resident.clone();

        tokio::spawn({
            async move {
                loop {
                    tokio::select! {
                        _ = token.cancelled() => {
                            break;
                        }
                        event = channel.recv() => match event {
                            Some(event) => resident_clone.handle_event(event).await,
                            None => break,

                        }
                    }
                }
            }
        });

        Ok(resident)
    }

    /// Create a `ResidentVec` for items matching the given query.
    pub fn resident_vec<T: Data + 'static>(
        &self,
        query: impl DataQuery<T>,
    ) -> Result<ResidentVec<T>> {
        let conditions = query.conditions();

        // Hold onto this transaction until we've created the watch channel so
        // we don't miss any updates.
        let r = self.0.r_transaction()?;
        let mut inner = BTreeMap::new();
        for item in query.query(r.scan())? {
            inner.insert(
                item.id(),
                Resident {
                    db: self.0.clone(),
                    inner: Arc::new(RwLock::new(item)),
                    watch: None,
                },
            );
        }

        // Ideally choose the most restrictive condition for the watcher
        let (mut channel, watch_id) = match conditions
            .first()
            .expect("There must be at least one condition")
        {
            DataCondition::Equal(key, value) => self
                .0
                .watch()
                .scan()
                .secondary(key.clone())
                .range::<T, _>(value.clone()..=value.clone())?,
            DataCondition::Range(key, value) => self
                .0
                .watch()
                .scan()
                .secondary(key.clone())
                .range::<T, _>(value.clone())?,
        };

        // Safe to end the transaction once the watcher is registered
        drop(r);

        let cancel_token = CancellationToken::new();
        let token = cancel_token.clone();

        let inner = Arc::new(RwLock::new(inner));
        let db_clone = self.0.clone();

        tokio::spawn({
            let inner_clone = Arc::clone(&inner);
            async move {
                'next_event: loop {
                    tokio::select! {
                        _ = token.cancelled() => {
                            break;
                        }
                        event = channel.recv() => match event {
                            Some(event) => match event {
                                Event::Insert(data) => match data.inner::<T>() {
                                    Ok(d) => {
                                        // The first condition should be satified because that's the watcher's condition
                                        assert!(conditions.first().expect("There must be at least one condition").check(&d));

                                        // Make sure the remaining conditions are satisfied
                                        for condition in conditions.iter().skip(1) {
                                            if !condition.check(&d) {
                                                continue 'next_event;
                                            }
                                        }

                                        let mut c = inner_clone.write().await;
                                        (*c).insert(d.id(), Resident { db: db_clone.clone(), inner: Arc::new(RwLock::new(d)), watch: None });
                                    },
                                    Err(_) => {},
                                }
                                Event::Update(data) => match data.inner_new::<T>() {
                                    Ok(d) => {
                                        let c = inner_clone.write().await;
                                        match (*c).get(&d.id()) {
                                            Some(r) => {
                                                let mut r = r.inner.write().await;
                                                *r = d;
                                            },
                                            // Got an update before insert?
                                            None => todo!(),
                                        }
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

        Ok(ResidentVec {
            inner,
            db: self.0.clone(),
            watch: (watch_id, cancel_token),
        })
    }
}

/// `Data` are rows in a database.
pub trait Data
where
    Self: ToInput + Clone + PartialEq + Send + Sync + Default + std::fmt::Debug,
{
    /// Get the unique identifier for this particular `Data`.
    fn id(&self) -> DataIdentifier;

    fn set_id(&mut self, id: DataIdentifier);

    fn revision(&self) -> DataRevision;

    fn set_revision(&mut self, revision: DataRevision);

    fn revision_key() -> KeyDefinition<KeyOptions> {
        KeyDefinition::new(
            Self::native_model_id(),
            Self::native_model_version(),
            "_revision",
            <DataRevision>::key_names(),
            KeyOptions {
                unique: false,
                optional: false,
            },
        )
    }

    fn creation(&self) -> DataCreation;

    fn creation_key() -> KeyDefinition<KeyOptions> {
        KeyDefinition::new(
            Self::native_model_id(),
            Self::native_model_version(),
            "_creation",
            <DataCreation>::key_names(),
            KeyOptions {
                unique: false,
                optional: false,
            },
        )
    }

    /// If an expiration timestamp is given, previous revisions of the `Data`
    /// will be kept until the expiration date. Otherwise, only the latest
    /// revision is kept.
    fn expiration(&self) -> Option<DataExpiration>;
}

// TODO Eq based only on inner?

/// Revisions are previous versions of `Data`.
#[derive(Eq, Debug, Clone, Copy, Hash)]
pub enum DataRevision {
    Latest(i64),
    Previous(i64),
}

impl Add<i64> for DataRevision {
    type Output = DataRevision;

    fn add(self, rhs: i64) -> Self::Output {
        match self {
            DataRevision::Latest(v) => DataRevision::Latest(v + rhs),
            DataRevision::Previous(v) => DataRevision::Previous(v + rhs),
        }
    }
}

impl From<DataRevision> for i64 {
    fn from(value: DataRevision) -> Self {
        match value {
            DataRevision::Latest(v) => v,
            DataRevision::Previous(v) => v,
        }
    }
}

impl PartialEq for DataRevision {
    fn eq(&self, other: &Self) -> bool {
        Into::<i64>::into(*self) == Into::<i64>::into(*other)
    }
}

impl Serialize for DataRevision {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: Serializer,
    {
        serializer.serialize_i64(match self {
            DataRevision::Latest(v) => *v,
            // Previous values are stored as negative so we can tell the difference
            DataRevision::Previous(v) => -v,
        })
    }
}

struct DataGenerationVisitor;

impl<'de> Visitor<'de> for DataGenerationVisitor {
    type Value = DataRevision;

    fn expecting(&self, formatter: &mut std::fmt::Formatter) -> std::fmt::Result {
        formatter.write_str("an i64")
    }

    fn visit_i64<E>(self, value: i64) -> Result<Self::Value, E>
    where
        E: serde::de::Error,
    {
        if value < 0 {
            Ok(DataRevision::Previous(-value))
        } else {
            Ok(DataRevision::Latest(value))
        }
    }
}

impl<'de> Deserialize<'de> for DataRevision {
    fn deserialize<D>(deserializer: D) -> Result<DataRevision, D::Error>
    where
        D: Deserializer<'de>,
    {
        deserializer.deserialize_i64(DataGenerationVisitor)
    }
}

impl Default for DataRevision {
    fn default() -> Self {
        Self::Latest(1)
    }
}

impl ToKey for DataRevision {
    fn to_key(&self) -> native_db::Key {
        native_db::Key::new(
            match self {
                DataRevision::Latest(v) => *v,
                // Previous values are stored as negative so we can tell the difference
                DataRevision::Previous(v) => -v,
            }
            .to_be_bytes()
            .to_vec(),
        )
    }

    fn key_names() -> Vec<String> {
        vec!["DataRevision".to_string()]
    }
}

/// Uniquely identifies a record in the database.
pub type DataIdentifier = u64;

/// When some `Data` will expire and no longer be returnable by queries.
/// Eventually it will be removed from the database altogether.
#[derive(Serialize, Deserialize, Debug, PartialEq, Eq, Copy, Clone)]
pub struct DataExpiration(DateTime<Utc>);

impl Default for DataExpiration {
    fn default() -> Self {
        Self(DateTime::<Utc>::MAX_UTC)
    }
}

impl ToKey for DataExpiration {
    fn to_key(&self) -> native_db::Key {
        native_db::Key::new(self.0.timestamp_millis().to_be_bytes().to_vec())
    }

    fn key_names() -> Vec<String> {
        vec!["DataExpiration".to_string()]
    }
}

/// When some `Data` was first added to the database. Modification times are not
/// tracked for individual `Data` because revisions handle that.
#[derive(Serialize, Deserialize, Debug, PartialEq, Eq, Copy, Clone)]
pub struct DataCreation(DateTime<Utc>);

impl Default for DataCreation {
    fn default() -> Self {
        Self(Utc::now())
    }
}

impl ToKey for DataCreation {
    fn to_key(&self) -> native_db::Key {
        native_db::Key::new(self.0.timestamp_millis().to_be_bytes().to_vec())
    }

    fn key_names() -> Vec<String> {
        vec!["DataCreation".to_string()]
    }
}

// TODO special case of ResVec?
/// Maintains a real-time cache of persistent objects in the database of
/// type `T`.
#[derive(Clone)]
pub struct Resident<T>
where
    T: Data,
{
    db: Arc<native_db::Database<'static>>,
    inner: Arc<RwLock<T>>,
    // listeners: Arc<RwLock<Vec>>,

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

    async fn handle_event(&self, event: Event) {
        match event {
            Event::Insert(data) => {}
            Event::Update(data) => match data.inner_new() {
                Ok(d) => {
                    // TODO check revision and discard updates from `update`
                    let mut c = self.inner.write().await;
                    *c = d;
                }
                Err(_) => {}
            },
            Event::Delete(_) => warn!("Deleting a singleton is undefined"),
        }
    }
}

impl<T: Data> Resident<T> {
    pub async fn update<F>(&self, mutator: F) -> Result<()>
    where
        F: Fn(&mut T) -> Result<()>,
    {
        let mut inner = self.inner.write().await;
        let mut next = inner.clone();
        mutator(&mut next)?;

        if next == *inner {
            trace!("Update did not change state");
            return Ok(());
        }
        assert_eq!(next.id(), inner.id(), "Primary key changed");

        // Bump revisions
        next.set_revision(next.revision() + 1);
        inner.set_revision(DataRevision::Previous(next.revision().into()));

        trace!(next = ?next, "Updated");

        let rw = self.db.rw_transaction()?;

        if inner.expiration().is_some() {
            // New id
            next.set_id(DataIdentifier::default());

            rw.insert(next.clone())?;
            rw.upsert(inner.clone())?;
        } else {
            rw.upsert(next.clone())?;
        }

        rw.commit()?;

        *inner = next;
        Ok(())
    }

    pub async fn history(&self, range: RangeFrom<DataRevision>) -> Result<Vec<T>> {
        // Get values of all secondary keys
        let mut secondary_keys = (*self.inner.read().await).native_db_secondary_keys();

        // Remove generation because we're going to set that one manually
        secondary_keys.retain(|key_def, _| *key_def != T::revision_key());

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
    use native_db::*;
    use native_model::Model;
    use sandpolis_macros::{Data, data};
    use serde::{Deserialize, Serialize};
    use tokio::time::{Duration, sleep};

    #[data]
    pub struct TestData {
        #[secondary_key]
        pub a: String,
        #[secondary_key]
        pub b: String,
    }

    #[data(temporal)]
    pub struct TestHistoryData {
        #[secondary_key]
        pub a: String,
        pub b: String,
    }

    #[tokio::test]
    #[test_log::test]
    async fn test_data() -> Result<()> {
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
        let res: Resident<TestData> = db.resident(DataCondition::equal(TestDataKey::a, "A"))?;

        // Update data a bunch of times
        for i in 1..10 {
            res.update(|data| {
                data.a = format!("test {i}");
                Ok(())
            })
            .await?;
        }

        // Resident should reflect "test 9"
        assert_eq!(res.read().await.a, "test 9");

        // Database should reflect "test 9"
        {
            let r = db.r_transaction()?;
            let items: Vec<TestData> = r.scan().primary()?.all()?.try_collect()?;
            assert_eq!(items.len(), 1);
            assert_eq!(items[0].a, "test 9");
        }

        // Change it externally
        {
            let rw = db.rw_transaction()?;
            rw.upsert(TestData {
                _id: res.read().await._id,
                _revision: DataRevision::default(),
                _creation: DataCreation::default(),
                a: "test 10".into(),
                b: "".into(),
            })?;
            rw.commit()?;
        }

        // Watch should reflect "test 10" after a while
        sleep(Duration::from_secs(1)).await;
        assert_eq!(res.read().await.a, "test 10");

        Ok(())
    }

    #[tokio::test]
    async fn test_temporal_data() -> Result<()> {
        let models = Box::leak(Box::new(Models::new()));
        models.define::<TestHistoryData>().unwrap();

        let database = DatabaseLayer::new(
            DatabaseConfig {
                storage: None,
                ephemeral: true,
            },
            models,
        )?;

        let db = database.realm(RealmName::default()).await?;
        let res: Resident<TestHistoryData> = db.resident(())?;

        // Update data a bunch of times
        for i in 1..10 {
            res.update(|data| {
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
            // assert_eq!(
            //     res.history(DbTimestamp::Latest(DateTime::default())..)
            //         .await?
            //         .len(),
            //     0
            // );
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
    inner: Arc<RwLock<BTreeMap<DataIdentifier, Resident<T>>>>,

    watch: (u64, CancellationToken),
}

impl<T: Data> Drop for ResidentVec<T> {
    fn drop(&mut self) {
        let (watch_id, token) = &self.watch;
        token.cancel();
        self.db.unwatch(*watch_id).unwrap();
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
    pub async fn push(&self, value: T) -> Result<Resident<T>> {
        let id = value.id();

        {
            let rw = self.db.rw_transaction()?;

            // Check for id collision
            if self.inner.read().await.get(&id).is_some() {
                bail!("Duplicate primary key");
            }

            // Insert and let watcher update `inner`
            rw.insert(value)?;
            rw.commit()?;
        }

        // TODO don't busy wait
        loop {
            if let Some(value) = self.inner.read().await.get(&id) {
                return Ok(value.clone());
            }
        }
    }
}

#[derive(Clone)]
pub enum DataCondition {
    Equal(KeyDefinition<KeyOptions>, Key),
    Range(KeyDefinition<KeyOptions>, KeyRange),
}

impl DataCondition {
    fn check<T: Data>(&self, data: &T) -> bool {
        match self {
            DataCondition::Equal(key, value) => {
                match data
                    .native_db_secondary_keys()
                    .get(key)
                    .expect("This key should exist")
                {
                    KeyEntry::Default(key) => *key == *value,
                    KeyEntry::Optional(key) => todo!(),
                }
            }
            DataCondition::Range(key, value) => {
                match data
                    .native_db_secondary_keys()
                    .get(key)
                    .expect("This key should exist")
                {
                    KeyEntry::Default(key) => value.contains(key),
                    KeyEntry::Optional(key) => todo!(),
                }
            }
        }
    }

    fn equal(key: impl ToKeyDefinition<KeyOptions>, value: impl ToKey) -> Self {
        Self::Equal(key.key_definition(), value.to_key())
    }

    fn range<R: RangeBounds<impl ToKey>>(key: impl ToKeyDefinition<KeyOptions>, value: R) -> Self {
        Self::Range(key.key_definition(), KeyRange::new(value))
    }
}

pub trait DataQuery<T: Data> {
    fn query(&self, scan: RScan) -> Result<Vec<T>>;
    fn conditions(&self) -> Vec<DataCondition>;

    fn check(&self, item: &T) -> bool {
        for condition in self.conditions() {
            if !condition.check(item) {
                return false;
            }
        }
        return true;
    }
}

macro_rules! collect_conditions {
    ($scan:ident, $condition:ident, $($conditions:ident),*) => {
        match condition.clone() {
            DataCondition::Equal(key, value) => scan.secondary(key)?.equal(value)?.try_collect()?,
            DataCondition::Range(key, value) => {
                let secondary = scan.secondary(key)?;
                let it = secondary.range(value)?;
                it.try_collect()?
            }
        }
    };
}

impl<T: Data> DataQuery<T> for () {
    fn query(&self, scan: RScan) -> Result<Vec<T>> {
        Ok(scan.primary()?.all()?.try_collect()?)
    }

    fn conditions(&self) -> Vec<DataCondition> {
        vec![]
    }
}

impl<T: Data> DataQuery<T> for DataCondition {
    fn query(&self, scan: RScan) -> Result<Vec<T>> {
        Ok(match self.clone() {
            DataCondition::Equal(key, value) => scan.secondary(key)?.equal(value)?.try_collect()?,
            DataCondition::Range(key, value) => scan.secondary(key)?.range(value)?.try_collect()?,
        })
    }

    fn conditions(&self) -> Vec<DataCondition> {
        vec![self.clone()]
    }
}

impl<T: Data> DataQuery<T> for (DataCondition, DataCondition) {
    fn query(&self, scan: RScan) -> Result<Vec<T>> {
        todo!()
    }

    fn conditions(&self) -> Vec<DataCondition> {
        vec![self.0.clone(), self.1.clone()]
    }
}

#[cfg(test)]
mod test_resident_vec {
    use super::*;
    use crate as sandpolis_database;
    use anyhow::Result;
    use native_db::Models;
    use native_db::*;
    use native_model::{Model, native_model};
    use sandpolis_macros::{Data, data};
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
        let test_data: ResidentVec<TestData> =
            db.resident_vec(DataCondition::equal(TestDataKey::a, "A"))?;

        assert_eq!(test_data.len().await, 0);

        // Add item
        let data: Resident<TestData> = test_data
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
                _revision: DataRevision::default(),
                _creation: DataCreation::default(),
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
