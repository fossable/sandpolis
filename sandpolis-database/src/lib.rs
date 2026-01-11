use crate::config::DatabaseConfig;
use anyhow::{Result, bail};
use chrono::{DateTime, Utc};
use native_db::db_type::{KeyDefinition, KeyEntry, KeyOptions, KeyRange, ToKeyDefinition};
use native_db::transaction::query::RScan;
use native_db::transaction::{RTransaction, RwTransaction};
use native_db::watch::Event;
use native_db::{Key, Models, ToInput, ToKey};
use rand::prelude::*;
use sandpolis_core::RealmName;
use serde::de::Visitor;
use serde::{Deserialize, Deserializer, Serialize, Serializer};
use std::collections::BTreeMap;
use std::collections::btree_map::IntoValues;
use std::ops::{Add, RangeBounds};
use std::sync::Arc;
use std::sync::{Mutex, RwLock, RwLockReadGuard};
use tokio_util::sync::CancellationToken;
use tracing::{debug, trace, warn};

pub mod cli;
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
        debug!("Initializing database layer");

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
    pub fn realm(&self, name: RealmName) -> Result<RealmDatabase> {
        {
            let inner = self.inner.read().unwrap();
            if let Some(db) = inner.get(&name) {
                return Ok(db.clone());
            }
        }

        let mut inner = self.inner.write().unwrap();

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

/// Create an in-memory database for testing.
#[macro_export]
macro_rules! test_db {
    ($($model:ident),+) => {{
        let models = Box::leak(Box::new(native_db::Models::new()));
        $(
            models.define::<$model>().unwrap();
        ),+

        sandpolis_database::DatabaseLayer::new(
            sandpolis_database::config::DatabaseConfig {
                storage: None,
                ephemeral: true,
            },
            models,
        )?
    }}
}

/// Database handle containing all `Data` for a particular realm.
#[derive(Clone)]
pub struct RealmDatabase(Arc<native_db::Database<'static>>);

impl RealmDatabase {
    fn new(inner: native_db::Database<'static>) -> Self {
        Self(Arc::new(inner))
    }

    /// Direct access to a new read-write transaction.
    pub fn rw_transaction(&self) -> Result<RwTransaction<'_>> {
        Ok(self.0.rw_transaction()?)
    }

    /// Direct access to a new read-only transaction.
    pub fn r_transaction(&self) -> Result<RTransaction<'_>> {
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
            // Watch exact primary ID because it can't change
            self.0.watch().get().primary::<T>(item.id())?
        } else {
            // Otherwise, watch the upper half of the primary ID which is the same for all
            // revisions
            self.0.watch().scan().primary().range::<T, _>(
                DataIdentifier((item.id().revision_id() as u64) << 32)
                    ..=DataIdentifier(((item.id().revision_id() as u64) << 32) | 0xFFFF_FFFF),
            )?
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
                            Some(event) => resident_clone.handle_event(event),
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
        let (mut channel, watch_id) = match conditions.first() {
            Some(DataCondition::Equal(key, value)) => self
                .0
                .watch()
                .scan()
                .secondary(key.clone())
                .range::<T, _>(value.clone()..=value.clone())?,
            Some(DataCondition::Range(key, value)) => self
                .0
                .watch()
                .scan()
                .secondary(key.clone())
                .range::<T, _>(value.clone())?,
            // TODO only latest revisions
            None => self.0.watch().scan().primary().all::<T>()?,
        };

        // Safe to end the transaction once the watcher is registered
        drop(r);

        let token = CancellationToken::new();
        let resident = ResidentVec {
            inner: Arc::new(RwLock::new(inner)),
            db: self.0.clone(),
            watch: (watch_id, token.clone()),
            conditions,
            listeners: Arc::new(Mutex::new(Vec::new())),
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
                            Some(event) => resident_clone.handle_event(event),
                            None => {
                                break;
                            }
                        }
                    }
                }
            }
        });

        Ok(resident)
    }
}

/// `Data` are rows in a database.
pub trait Data
where
    Self: ToInput + Clone + PartialEq + Send + Sync + std::fmt::Debug,
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
#[derive(Eq, Debug, Clone, Copy)]
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

impl PartialOrd for DataRevision {
    fn partial_cmp(&self, other: &Self) -> Option<std::cmp::Ordering> {
        Some(self.cmp(other))
    }
}

impl Ord for DataRevision {
    fn cmp(&self, other: &Self) -> std::cmp::Ordering {
        Into::<i64>::into(*self).cmp(&Into::<i64>::into(*other))
    }
}

impl std::hash::Hash for DataRevision {
    fn hash<H: std::hash::Hasher>(&self, state: &mut H) {
        Into::<i64>::into(*self).hash(state);
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
        match self {
            DataRevision::Latest(v) => *v,
            // Previous values are stored as negative so we can tell the difference
            DataRevision::Previous(v) => -v,
        }
        .to_key()
    }

    fn key_names() -> Vec<String> {
        vec!["DataRevision".to_string()]
    }
}

/// Uniquely identifies a record in the database. All revisions of the same
/// `Data` share the upper 4 bytes of this value.
#[derive(Serialize, Deserialize, Debug, PartialEq, Eq, Copy, Clone, PartialOrd, Ord)]
pub struct DataIdentifier(u64);

impl Default for DataIdentifier {
    fn default() -> Self {
        Self(rand::rng().next_u64())
    }
}

impl ToKey for DataIdentifier {
    fn to_key(&self) -> native_db::Key {
        self.0.to_key()
    }

    fn key_names() -> Vec<String> {
        vec!["DataIdentifier".to_string()]
    }
}

impl DataIdentifier {
    /// Create a new `DataIdentifier` for a revision based on this one.
    fn new_revision(&self) -> Self {
        Self((self.0 & 0xFFFF_FFFF_0000_0000) | (0x0000_0000_FFFF_FFFF & rand::rng().next_u64()))
    }

    /// Get the portion of the `DataIdentifier` shared across all revisions
    fn revision_id(&self) -> u32 {
        ((self.0 >> 32) & 0xFFFF_FFFF) as u32
    }
}

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
        self.0.timestamp_millis().to_key()
    }

    fn key_names() -> Vec<String> {
        vec!["DataExpiration".to_string()]
    }
}

/// When some `Data` was first added to the database. Modification times are not
/// tracked for individual `Data` because revisions handle that.
#[derive(Serialize, Deserialize, Debug, PartialEq, Eq, Copy, Clone)]
pub struct DataCreation(DateTime<Utc>);

impl DataCreation {
    pub fn all() -> impl RangeBounds<Self> {
        trace!("{:?}", Self(DateTime::<Utc>::MIN_UTC).to_key());
        trace!("{:?}", Self(DateTime::<Utc>::MAX_UTC).to_key());
        Self(DateTime::<Utc>::MIN_UTC)..=Self(DateTime::<Utc>::MAX_UTC)
    }
}

impl Default for DataCreation {
    fn default() -> Self {
        Self(Utc::now())
    }
}

impl ToKey for DataCreation {
    fn to_key(&self) -> native_db::Key {
        self.0.timestamp_millis().to_key()
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

impl<T: Data> Resident<T> {
    pub fn read(&self) -> RwLockReadGuard<'_, T> {
        self.inner.read().unwrap()
    }

    fn handle_event(&self, event: Event) {
        trace!(event = ?event, "Handling event");
        match event {
            Event::Insert(_data) => {}
            Event::Update(data) => if let Ok(d) = data.inner_new::<T>() {
                let mut c = self.inner.write().unwrap();
                // Only accept updates with newer or equal revisions (last write wins)
                if d.revision() >= c.revision() {
                    *c = d;
                } else {
                    trace!(
                        incoming_rev = ?d.revision(),
                        current_rev = ?c.revision(),
                        "Discarding update with older revision"
                    );
                }
            },
            Event::Delete(_) => warn!("Deleting a singleton is undefined"),
        }
    }
}

impl<T: Data> Resident<T> {
    pub fn update<F>(&self, mutator: F) -> Result<()>
    where
        F: Fn(&mut T) -> Result<()>,
    {
        let mut previous = self.inner.write().unwrap();
        let mut next = previous.clone();
        mutator(&mut next)?;

        if next == *previous {
            trace!("Update did not change state");
            return Ok(());
        }
        assert_eq!(next.id(), previous.id(), "Primary key changed");

        // Bump revisions
        next.set_revision(next.revision() + 1);
        previous.set_revision(DataRevision::Previous(next.revision().into()));

        trace!(next = ?next, "Updated");

        let rw = self.db.rw_transaction()?;

        if previous.expiration().is_some() {
            // Derive new id from the previous
            next.set_id(previous.id().new_revision());

            rw.insert(next.clone())?;
            rw.upsert(previous.clone())?;
        } else {
            rw.upsert(next.clone())?;
        }

        rw.commit()?;

        *previous = next;
        Ok(())
    }

    pub fn history(&self, range: impl RangeBounds<DataCreation>) -> Result<Vec<T>> {
        let revision_id = self.read().id().revision_id();

        let r = self.db.r_transaction()?;

        // TODO .and_secondary instead of fully deserializing
        let items: Vec<T> = r
            .scan()
            .secondary(T::creation_key())?
            .range(range)?
            .collect::<Result<Vec<_>, _>>()?;

        trace!("{} items", items.len());

        Ok(items
            .into_iter()
            .filter(|item| item.id().revision_id() == revision_id)
            .collect())
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
    use tokio::time::{Duration, sleep};

    #[data]
    #[derive(Default)]
    pub struct TestData {
        #[secondary_key]
        pub a: String,
        #[secondary_key]
        pub b: String,
    }

    #[data(temporal)]
    #[derive(Default)]
    pub struct TestHistoryData {
        #[secondary_key]
        pub a: String,
        pub b: String,
    }

    #[tokio::test]
    #[test_log::test]
    async fn test_data() -> Result<()> {
        let database = test_db!(TestData);

        let db = database.realm(RealmName::default())?;
        let res: Resident<TestData> = db.resident(DataCondition::equal(TestDataKey::a, "A"))?;

        // Update data a bunch of times
        for i in 1..10 {
            res.update(|data| {
                data.a = format!("test {i}");
                Ok(())
            })?;
        }

        // Resident should reflect "test 9"
        assert_eq!(res.read().a, "test 9");

        // Database should reflect "test 9"
        {
            let r = db.r_transaction()?;
            let items: Vec<TestData> = r.scan().primary()?.all()?.collect::<Result<Vec<_>, _>>()?;
            assert_eq!(items.len(), 1);
            assert_eq!(items[0].a, "test 9");
        }

        // Change it externally
        {
            let rw = db.rw_transaction()?;
            rw.upsert(TestData {
                _id: res.read()._id,
                _revision: DataRevision::Latest(10), // Newer than internal revision 9
                _creation: DataCreation::default(),
                a: "test 10".into(),
                b: "".into(),
            })?;
            rw.commit()?;
        }

        // Watch should reflect "test 10" after a while
        sleep(Duration::from_secs(1)).await;
        assert_eq!(res.read().a, "test 10");

        Ok(())
    }

    #[tokio::test]
    #[test_log::test]
    async fn test_temporal_data() -> Result<()> {
        let database = test_db!(TestHistoryData);

        let db = database.realm(RealmName::default())?;
        let res: Resident<TestHistoryData> = db.resident(())?;

        // Update data 9 times
        for i in 1..10 {
            res.update(|data| {
                data.b = format!("test {i}");
                Ok(())
            })?;
        }

        // Database should have 10 items
        {
            let r = db.r_transaction()?;
            let items: Vec<TestHistoryData> =
                r.scan().primary()?.all()?.collect::<Result<Vec<_>, _>>()?;
            assert_eq!(items.len(), 10);
        }

        // Check history
        {
            assert_eq!(res.history(DataCreation::all())?.len(), 10);
        }

        Ok(())
    }
}

/// Events that can occur on a ResidentVec
#[derive(Clone)]
pub enum ResidentVecEvent<T>
where
    T: Data,
{
    /// A new entry was added
    Added(Resident<T>),
    /// An existing entry was updated  
    Updated(Resident<T>),
    /// An entry was removed
    Removed(DataIdentifier),
}

impl<T: Data> std::fmt::Debug for ResidentVecEvent<T> {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            ResidentVecEvent::Added(_) => f.debug_tuple("Added").field(&"<Resident>").finish(),
            ResidentVecEvent::Updated(_) => f.debug_tuple("Updated").field(&"<Resident>").finish(),
            ResidentVecEvent::Removed(id) => f.debug_tuple("Removed").field(id).finish(),
        }
    }
}

/// Type alias for listener functions
pub type ResidentVecListener<T> = Arc<dyn Fn(ResidentVecEvent<T>) + Send + Sync>;

#[derive(Clone)]
pub struct ResidentVec<T>
where
    T: Data,
{
    db: Arc<native_db::Database<'static>>,
    inner: Arc<RwLock<BTreeMap<DataIdentifier, Resident<T>>>>,
    conditions: Vec<DataCondition>,
    listeners: Arc<Mutex<Vec<ResidentVecListener<T>>>>,

    watch: (u64, CancellationToken),
}

impl<T: Data> Drop for ResidentVec<T> {
    fn drop(&mut self) {
        let (watch_id, token) = &self.watch;
        token.cancel();
        self.db.unwatch(*watch_id).unwrap();
    }
}

impl<T: Data> ResidentVec<T> {
    fn notify_listeners(&self, event: ResidentVecEvent<T>) {
        if let Ok(listeners) = self.listeners.lock() {
            for listener in listeners.iter() {
                listener(event.clone());
            }
        }
    }

    fn handle_event(&self, event: Event) {
        trace!(event = ?event, "Handling event");
        match event {
            Event::Insert(insert) => if let Ok(data) = insert.inner::<T>() {
                // The first condition should be satified because that's the watcher's condition
                assert!(
                    self.conditions
                        .first()
                        .map(|condition| condition.check(&data))
                        .unwrap_or(true)
                );

                // Make sure the remaining conditions are satisfied
                for condition in self.conditions.iter().skip(1) {
                    if !condition.check(&data) {
                        return;
                    }
                }

                let data_id = data.id();
                let mut c = self.inner.write().unwrap();

                // Check if this item already exists (inserted by push())
                if let Some(existing) = (*c).get(&data_id) {
                    // If it exists and has the same revision, skip notification AND update
                    // (push() already notified listeners and inserted the Resident)
                    if existing.read().revision() == data.revision() {
                        trace!(
                            id = ?data_id,
                            revision = ?data.revision(),
                            "Skipping duplicate insert"
                        );
                        return;
                    } else {
                        // Different revision - this shouldn't happen for Insert events
                        warn!(
                            existing_rev = ?existing.read().revision(),
                            new_rev = ?data.revision(),
                            "Insert event for existing item with different revision"
                        );
                    }
                }

                let resident = Resident {
                    db: self.db.clone(),
                    inner: Arc::new(RwLock::new(data)),
                    watch: None,
                };

                (*c).insert(data_id, resident.clone());
                drop(c);

                // Notify listeners
                self.notify_listeners(ResidentVecEvent::Added(resident));
            },
            Event::Update(insert) => if let Ok(data) = insert.inner_new::<T>() {
                let mut c = self.inner.write().unwrap();
                match (*c).get(&data.id()) {
                    Some(r) => {
                        let mut r_inner = r.inner.write().unwrap();
                        // Only accept updates with newer or equal revisions
                        if data.revision() >= r_inner.revision() {
                            trace!(
                                incoming_rev = ?data.revision(),
                                current_rev = ?r_inner.revision(),
                                new_value = ?data,
                                "Applying update to ResidentVec item"
                            );
                            *r_inner = data;
                            drop(r_inner);

                            // Notify listeners
                            self.notify_listeners(ResidentVecEvent::Updated(r.clone()));
                        } else {
                            trace!(
                                incoming_rev = ?data.revision(),
                                current_rev = ?r_inner.revision(),
                                "Discarding update with older revision"
                            );
                        }
                    }
                    // Got an update before insert - treat as initial insert
                    None => {
                        trace!(
                            id = ?data.id(),
                            revision = ?data.revision(),
                            "Got update before insert, treating as initial insert"
                        );
                        let data_id = data.id();
                        let resident = Resident {
                            db: self.db.clone(),
                            inner: Arc::new(RwLock::new(data)),
                            watch: None,
                        };

                        (*c).insert(data_id, resident.clone());
                        drop(c);

                        // Notify listeners as an addition
                        self.notify_listeners(ResidentVecEvent::Added(resident));
                    }
                }
            },
            Event::Delete(delete) => if let Ok(data) = delete.inner::<T>() {
                let data_id = data.id();
                let mut c = self.inner.write().unwrap();
                if let Some(_removed) = (*c).remove(&data_id) {
                    trace!(
                        id = ?data_id,
                        "Removing item from ResidentVec (from watcher)"
                    );
                    drop(c);
                    // Notify listeners
                    self.notify_listeners(ResidentVecEvent::Removed(data_id));
                } else {
                    trace!(
                        id = ?data_id,
                        "Delete event for already-removed item (remove() was called directly)"
                    );
                }
            },
        }
    }
}

// Replicate some of the Vec API
impl<T: Data> ResidentVec<T> {
    /// Returns the number of elements in the vector, also referred to
    /// as its 'length'.
    pub fn len(&self) -> usize {
        self.inner.read().unwrap().len()
    }

    /// Appends an element to the back of a collection.
    pub fn push(&self, value: T) -> Result<Resident<T>> {
        let id = value.id();
        let resident = Resident {
            db: self.db.clone(),
            inner: Arc::new(RwLock::new(value.clone())),
            watch: None,
        };

        let result = {
            let rw = self.db.rw_transaction()?;

            // Check for id collision
            if self.inner.read().unwrap().get(&id).is_some() {
                bail!("Duplicate primary key");
            }

            // Watcher should ignore by revision
            let mut inner = self.inner.write().unwrap();
            (*inner).insert(id, resident);
            let result = (*inner).get(&id).unwrap().clone();
            drop(inner);

            rw.insert(value)?;
            rw.commit()?;

            result
        };

        // Notify listeners immediately
        self.notify_listeners(ResidentVecEvent::Added(result.clone()));

        Ok(result)
    }

    /// Removes an element from the collection by its ID.
    pub fn remove(&self, id: DataIdentifier) -> Result<()> {
        let rw = self.db.rw_transaction()?;

        // Check if the item exists
        let inner = self.inner.read().unwrap();
        let resident = inner.get(&id);

        if let Some(resident) = resident {
            // Remove from database
            let data = resident.inner.read().unwrap().clone();
            drop(inner);
            rw.remove(data)?;
            rw.commit()?;
            // Note: The watcher will handle removing from internal map and notifying listeners
        } else {
            bail!("Item not found");
        }

        Ok(())
    }

    pub fn iter(&self) -> IntoValues<DataIdentifier, Resident<T>> {
        self.inner.read().unwrap().clone().into_values()
    }

    pub fn stream(&self) -> futures::stream::Iter<IntoValues<DataIdentifier, Resident<T>>> {
        futures::stream::iter(self.inner.read().unwrap().clone().into_values())
    }

    /// Register a listener that will be called when entries are added, removed,
    /// or updated.
    pub fn listen<F>(&self, listener: F)
    where
        F: Fn(ResidentVecEvent<T>) + Send + Sync + 'static,
    {
        if let Ok(mut listeners) = self.listeners.lock() {
            listeners.push(Arc::new(listener));
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
                    KeyEntry::Optional(Some(key)) => *key == *value,
                    KeyEntry::Optional(None) => false, // None never equals any value
                }
            }
            DataCondition::Range(key, value) => {
                match data
                    .native_db_secondary_keys()
                    .get(key)
                    .expect("This key should exist")
                {
                    KeyEntry::Default(key) => value.contains(key),
                    KeyEntry::Optional(Some(key)) => value.contains(key),
                    KeyEntry::Optional(None) => false, // None is not in any range
                }
            }
        }
    }

    pub fn equal(key: impl ToKeyDefinition<KeyOptions>, value: impl ToKey) -> Self {
        Self::Equal(key.key_definition(), value.to_key())
    }

    pub fn range<R: RangeBounds<impl ToKey>>(
        key: impl ToKeyDefinition<KeyOptions>,
        value: R,
    ) -> Self {
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
        true
    }
}

macro_rules! collect_conditions {
    ($scan:ident, $condition:ident, $($conditions:ident),*) => {
        match condition.clone() {
            DataCondition::Equal(key, value) => scan
                .secondary(key)?
                .equal(value)?
                .collect::<Result<Vec<_>, _>>()?,
            DataCondition::Range(key, value) => {
                let secondary = scan.secondary(key)?;
                let it = secondary.range(value)?;
                it.collect::<Result<Vec<_>, _>>()?
            }
        }
    };
}

impl<T: Data> DataQuery<T> for () {
    fn query(&self, scan: RScan) -> Result<Vec<T>> {
        Ok(scan.primary()?.all()?.collect::<Result<Vec<_>, _>>()?)
    }

    fn conditions(&self) -> Vec<DataCondition> {
        vec![]
    }
}

impl<T: Data> DataQuery<T> for DataCondition {
    fn query(&self, scan: RScan) -> Result<Vec<T>> {
        Ok(match self.clone() {
            DataCondition::Equal(key, value) => scan
                .secondary(key)?
                .equal(value)?
                .collect::<Result<Vec<_>, _>>()?,
            DataCondition::Range(key, value) => scan
                .secondary(key)?
                .range(value)?
                .collect::<Result<Vec<_>, _>>()?,
        })
    }

    fn conditions(&self) -> Vec<DataCondition> {
        vec![self.clone()]
    }
}

impl<T: Data> DataQuery<T> for (DataCondition, DataCondition) {
    fn query(&self, scan: RScan) -> Result<Vec<T>> {
        // For compound queries, we need to:
        // 1. Execute a query for the first condition
        // 2. Filter results by the second condition in memory
        // This is necessary because native_db doesn't support compound secondary key queries
        let items = self.0.query(scan)?;
        Ok(items
            .into_iter()
            .filter(|item| self.1.check(item))
            .collect())
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
    #[derive(Default)]
    pub struct TestData {
        #[secondary_key]
        pub a: String,
        #[secondary_key]
        pub b: String,
    }

    #[tokio::test]
    #[test_log::test]
    async fn test_nonhistorical() -> Result<()> {
        let database = test_db!(TestData);

        let db = database.realm(RealmName::default())?;
        let test_data: ResidentVec<TestData> =
            db.resident_vec(DataCondition::equal(TestDataKey::b, "B"))?;

        assert_eq!(test_data.len(), 0);

        // Add item
        let data: Resident<TestData> = test_data.push(TestData {
            a: "A".to_string(),
            b: "B".to_string(),
            ..Default::default()
        })?;

        assert_eq!(test_data.len(), 1);

        // Update a bunch of times
        for i in 1..10 {
            data.update(|d| {
                d.a = format!("test {i}");
                Ok(())
            })?;
        }

        // Database should reflect "test 9"
        {
            let r = db.r_transaction()?;
            let items: Vec<TestData> = r.scan().primary()?.all()?.collect::<Result<Vec<_>, _>>()?;
            assert_eq!(items.len(), 1);
            assert_eq!(items[0].a, "test 9");
        }

        // Change it in the database
        {
            let rw = db.rw_transaction()?;
            rw.upsert(TestData {
                _id: data.read()._id,
                _revision: DataRevision::Latest(10), // Newer than internal revision 9
                _creation: DataCreation::default(),
                a: "test 10".into(),
                b: "B".into(),
            })?;
            rw.commit()?;
        }

        // Should reflect "test 10" after a while
        sleep(Duration::from_secs(1)).await;

        // Note: The Resident returned by push() does not automatically see external updates
        // because it doesn't have its own watcher. Only the Resident stored in the ResidentVec
        // is updated. We verify that the revision was correctly updated to 10.
        assert_eq!(data.read().revision(), DataRevision::Latest(10));

        Ok(())
    }

    #[tokio::test]
    #[test_log::test]
    async fn test_resident_vec_listen() -> Result<()> {
        use std::sync::atomic::{AtomicUsize, Ordering};

        let database = test_db!(TestData);
        let db = database.realm(RealmName::default())?;
        let resident_vec: ResidentVec<TestData> = db.resident_vec(())?;

        let add_count = Arc::new(AtomicUsize::new(0));
        let update_count = Arc::new(AtomicUsize::new(0));

        let add_count_clone = add_count.clone();
        let update_count_clone = update_count.clone();

        // Register a listener
        resident_vec.listen(move |event| {
            match event {
                ResidentVecEvent::Added(_) => {
                    add_count_clone.fetch_add(1, Ordering::SeqCst);
                }
                ResidentVecEvent::Updated(_) => {
                    update_count_clone.fetch_add(1, Ordering::SeqCst);
                }
                ResidentVecEvent::Removed(_) => {
                    // Not implemented in current code
                }
            }
        });

        // Add some data
        let resident1 = resident_vec.push(TestData {
            a: "test1".to_string(),
            b: "value1".to_string(),
            ..Default::default()
        })?;

        let resident2 = resident_vec.push(TestData {
            a: "test2".to_string(),
            b: "value2".to_string(),
            ..Default::default()
        })?;

        // Give a moment for events to propagate
        tokio::time::sleep(tokio::time::Duration::from_millis(10)).await;

        // Update some data
        resident1.update(|data| {
            data.b = "updated_value1".to_string();
            Ok(())
        })?;

        resident2.update(|data| {
            data.b = "updated_value2".to_string();
            Ok(())
        })?;

        // Give a moment for events to propagate
        tokio::time::sleep(tokio::time::Duration::from_millis(10)).await;

        // Check that listeners were called
        assert_eq!(add_count.load(Ordering::SeqCst), 2);
        assert_eq!(update_count.load(Ordering::SeqCst), 2);

        Ok(())
    }

    #[tokio::test]
    #[test_log::test]
    async fn test_resident_vec_remove() -> Result<()> {
        use std::sync::atomic::{AtomicUsize, Ordering};

        let database = test_db!(TestData);
        let db = database.realm(RealmName::default())?;
        let resident_vec: ResidentVec<TestData> = db.resident_vec(())?;

        let remove_count = Arc::new(AtomicUsize::new(0));
        let remove_count_clone = remove_count.clone();

        // Register a listener
        resident_vec.listen(move |event| {
            if let ResidentVecEvent::Removed(_) = event {
                remove_count_clone.fetch_add(1, Ordering::SeqCst);
            }
        });

        // Add some data
        let resident1 = resident_vec.push(TestData {
            a: "test1".to_string(),
            b: "value1".to_string(),
            ..Default::default()
        })?;

        let resident2 = resident_vec.push(TestData {
            a: "test2".to_string(),
            b: "value2".to_string(),
            ..Default::default()
        })?;

        assert_eq!(resident_vec.len(), 2);

        // Remove one item
        let id1 = resident1.read().id();
        resident_vec.remove(id1)?;

        // Give a moment for events to propagate
        tokio::time::sleep(tokio::time::Duration::from_millis(10)).await;

        assert_eq!(resident_vec.len(), 1);
        assert_eq!(remove_count.load(Ordering::SeqCst), 1);

        // Verify the remaining item is resident2
        let id2 = resident2.read().id();
        let items: Vec<_> = resident_vec.iter().collect();
        assert_eq!(items.len(), 1);
        assert_eq!(items[0].read().id(), id2);

        // Try to remove non-existent item
        assert!(resident_vec.remove(id1).is_err());

        Ok(())
    }

    #[tokio::test]
    #[test_log::test]
    async fn test_data_query_range() -> Result<()> {
        let database = test_db!(TestData);
        let db = database.realm(RealmName::default())?;

        // Insert test data with different values for 'a'
        {
            let rw = db.rw_transaction()?;
            rw.insert(TestData {
                a: "aaa".to_string(),
                b: "B1".to_string(),
                ..Default::default()
            })?;
            rw.insert(TestData {
                a: "bbb".to_string(),
                b: "B2".to_string(),
                ..Default::default()
            })?;
            rw.insert(TestData {
                a: "ccc".to_string(),
                b: "B3".to_string(),
                ..Default::default()
            })?;
            rw.insert(TestData {
                a: "ddd".to_string(),
                b: "B4".to_string(),
                ..Default::default()
            })?;
            rw.commit()?;
        }

        // Query with range on secondary key 'a'
        let resident_vec: ResidentVec<TestData> = db.resident_vec(DataCondition::range(
            TestDataKey::a,
            "bbb".to_string()..="ccc".to_string(),
        ))?;

        // Should get items with a="bbb" and a="ccc"
        assert_eq!(resident_vec.len(), 2);

        let items: Vec<_> = resident_vec
            .iter()
            .map(|r| r.read().a.clone())
            .collect::<Vec<_>>();

        assert!(items.contains(&"bbb".to_string()));
        assert!(items.contains(&"ccc".to_string()));

        Ok(())
    }

    #[tokio::test]
    #[test_log::test]
    async fn test_error_duplicate_key() -> Result<()> {
        let database = test_db!(TestData);
        let db = database.realm(RealmName::default())?;
        let resident_vec: ResidentVec<TestData> = db.resident_vec(())?;

        // Create item with specific ID
        let item = TestData {
            _id: DataIdentifier(12345),
            a: "test1".to_string(),
            b: "value1".to_string(),
            ..Default::default()
        };

        resident_vec.push(item.clone())?;

        // Try to push item with same ID
        let result = resident_vec.push(item);
        assert!(result.is_err());
        if let Err(e) = result {
            assert!(e.to_string().contains("Duplicate"));
        }

        Ok(())
    }

    #[tokio::test]
    #[test_log::test]
    async fn test_data_revision_ordering() -> Result<()> {
        // Test that revisions are properly ordered
        assert!(DataRevision::Latest(2) > DataRevision::Latest(1));
        assert!(DataRevision::Latest(10) > DataRevision::Latest(9));
        assert!(DataRevision::Latest(5) == DataRevision::Latest(5));

        // Previous revisions should compare by their numeric value
        assert!(DataRevision::Previous(2) > DataRevision::Previous(1));

        // Latest and Previous with same number should be equal
        assert!(DataRevision::Latest(5) == DataRevision::Previous(5));

        Ok(())
    }

    #[tokio::test]
    #[test_log::test]
    async fn test_concurrent_updates() -> Result<()> {
        let database = test_db!(TestData);
        let db = database.realm(RealmName::default())?;
        let resident: Resident<TestData> = db.resident(())?;

        // Spawn multiple tasks that update concurrently
        let mut handles = vec![];
        for i in 0..10 {
            let r = resident.clone();
            let handle = tokio::spawn(async move {
                r.update(|data| {
                    data.a = format!("concurrent_{}", i);
                    Ok(())
                })
            });
            handles.push(handle);
        }

        // Wait for all updates
        for handle in handles {
            handle.await.unwrap()?;
        }

        // Verify final revision is 11 (initial 1 + 10 updates)
        assert_eq!(resident.read().revision(), DataRevision::Latest(11));

        // Verify the value is one of the concurrent updates
        assert!(resident.read().a.starts_with("concurrent_"));

        Ok(())
    }

    #[tokio::test]
    #[test_log::test]
    async fn test_empty_query() -> Result<()> {
        let database = test_db!(TestData);
        let db = database.realm(RealmName::default())?;

        // Query with empty condition should return all items
        let resident_vec: ResidentVec<TestData> = db.resident_vec(())?;
        assert_eq!(resident_vec.len(), 0);

        // Add some items
        resident_vec.push(TestData {
            a: "test1".to_string(),
            b: "B".to_string(),
            ..Default::default()
        })?;

        resident_vec.push(TestData {
            a: "test2".to_string(),
            b: "B".to_string(),
            ..Default::default()
        })?;

        // New query should see both items
        let resident_vec2: ResidentVec<TestData> = db.resident_vec(())?;
        assert_eq!(resident_vec2.len(), 2);

        Ok(())
    }

    #[tokio::test]
    #[test_log::test]
    async fn test_resident_history() -> Result<()> {
        use crate::test_resident::TestHistoryData;

        let database = test_db!(TestHistoryData);
        let db = database.realm(RealmName::default())?;
        let resident: Resident<TestHistoryData> = db.resident(())?;

        // Make several updates
        for i in 1..=5 {
            resident.update(|data| {
                data.b = format!("version_{}", i);
                Ok(())
            })?;
        }

        // Get all history
        let history = resident.history(DataCreation::all())?;
        assert_eq!(history.len(), 6); // Initial + 5 updates

        // Verify versions are in history
        let values: Vec<String> = history.iter().map(|d| d.b.clone()).collect();
        assert!(values.contains(&"version_1".to_string()));
        assert!(values.contains(&"version_5".to_string()));

        Ok(())
    }
}
