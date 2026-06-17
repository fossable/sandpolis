//! Type-erased database replication.
//!
//! A [`SyncRegistry`] knows how to, for each registered [`Data`] type:
//! - apply an incoming [`SyncRecord`] to a local database (upsert/delete),
//! - produce a snapshot of matching records, and
//! - watch the database and emit matching records as they change.
//!
//! This is the engine behind the `SyncStream` (see `network::sync`): a responder
//! uses `snapshot` + `spawn_watch` to serve data; a requester uses `apply` to
//! ingest it. The wire is cbor-encoded `Data` keyed by `native_model_id`, so the
//! mechanism is generic over every layer's data.

use super::{Data, RealmDatabase};
use crate::InstanceId;
use anyhow::Result;
use native_db::watch::Event;
use native_model::Model;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::LazyLock;
use tokio::sync::mpsc::Sender;
use tokio_util::sync::CancellationToken;

#[derive(Clone, Copy, Serialize, Deserialize, Debug, PartialEq, Eq)]
pub enum SyncOp {
    Upsert,
    Delete,
}

/// A single replicated database record.
#[derive(Clone, Serialize, Deserialize, Debug)]
pub struct SyncRecord {
    pub model_id: u32,
    pub op: SyncOp,
    pub bytes: Vec<u8>,
}

/// Describes a subset of data a `SyncStream` cares about.
#[derive(Clone, Serialize, Deserialize, Debug, Default)]
pub struct SyncFilter {
    /// Restrict to a single model; `None` matches every registered model.
    pub model_id: Option<u32>,
    /// Restrict to a single instance's data; `None` matches every instance.
    ///
    /// Serialized as a string because the wire codec (cbor) cannot represent the
    /// 128-bit `InstanceId`.
    #[serde(with = "instance_id_opt")]
    pub instance: Option<InstanceId>,
}

mod instance_id_opt {
    use crate::InstanceId;
    use serde::{Deserialize, Deserializer, Serialize, Serializer};
    use std::str::FromStr;

    pub fn serialize<S: Serializer>(
        value: &Option<InstanceId>,
        serializer: S,
    ) -> Result<S::Ok, S::Error> {
        value.map(|id| id.to_string()).serialize(serializer)
    }

    pub fn deserialize<'de, D: Deserializer<'de>>(
        deserializer: D,
    ) -> Result<Option<InstanceId>, D::Error> {
        Option::<String>::deserialize(deserializer)?
            .map(|s| InstanceId::from_str(&s))
            .transpose()
            .map_err(serde::de::Error::custom)
    }
}

impl SyncFilter {
    /// A filter matching the entire database.
    pub fn all() -> Self {
        Self::default()
    }
}

type ApplyFn = Box<dyn Fn(&RealmDatabase, SyncOp, &[u8]) -> Result<()> + Send + Sync>;
type SnapshotFn =
    Box<dyn Fn(&RealmDatabase, Option<InstanceId>) -> Result<Vec<SyncRecord>> + Send + Sync>;
type WatchFn = Box<
    dyn Fn(&RealmDatabase, Option<InstanceId>, Sender<SyncRecord>, CancellationToken) -> Result<()>
        + Send
        + Sync,
>;

pub struct SyncType {
    apply: ApplyFn,
    snapshot: SnapshotFn,
    spawn_watch: WatchFn,
}

#[derive(Default)]
pub struct SyncRegistry {
    types: HashMap<u32, SyncType>,
}

impl SyncRegistry {
    pub fn new() -> Self {
        Self::default()
    }

    /// Register a type with no instance scoping. Instance-filtered subscriptions
    /// will never match this type (it has no owning instance).
    pub fn register<T>(&mut self)
    where
        T: Data + Model + 'static,
    {
        self.register_inner::<T>(None);
    }

    /// Register a type whose records belong to an instance, extracted by
    /// `instance_of` (typically `|d| d._instance_id`).
    pub fn register_scoped<T>(&mut self, instance_of: fn(&T) -> InstanceId)
    where
        T: Data + Model + 'static,
    {
        self.register_inner::<T>(Some(instance_of));
    }

    fn register_inner<T>(&mut self, instance_of: Option<fn(&T) -> InstanceId>)
    where
        T: Data + Model + 'static,
    {
        let model_id = T::native_model_id();

        let apply: ApplyFn = Box::new(|db, op, bytes| {
            let (item, _): (T, u32) = native_model::decode(bytes.to_vec())
                .map_err(|e| anyhow::anyhow!("decode failed: {e}"))?;
            let rw = db.rw_transaction()?;
            match op {
                SyncOp::Upsert => {
                    rw.upsert(item)?;
                }
                SyncOp::Delete => {
                    // Ignore "not found" so deletes are idempotent.
                    let _ = rw.remove(item);
                }
            }
            rw.commit()?;
            Ok(())
        });

        let snapshot: SnapshotFn = Box::new(move |db, instance| {
            let r = db.r_transaction()?;
            let items: Vec<T> = r
                .scan()
                .primary::<T>()?
                .all()?
                .collect::<std::result::Result<Vec<_>, _>>()?;
            drop(r);

            let mut out = Vec::new();
            for item in items {
                if !instance_matches(instance, instance_of, &item) {
                    continue;
                }
                out.push(SyncRecord {
                    model_id,
                    op: SyncOp::Upsert,
                    bytes: native_model::encode(&item)
                        .map_err(|e| anyhow::anyhow!("encode failed: {e}"))?,
                });
            }
            Ok(out)
        });

        let spawn_watch: WatchFn = Box::new(move |db, instance, tx, cancel| {
            let (mut channel, watch_id) = db.db().watch().scan().primary().all::<T>()?;
            let db = db.clone();
            tokio::spawn(async move {
                loop {
                    tokio::select! {
                        _ = cancel.cancelled() => break,
                        event = channel.recv() => match event {
                            Some(event) => {
                                if let Some(record) =
                                    event_to_record::<T>(event, model_id, instance, instance_of)
                                {
                                    if tx.send(record).await.is_err() {
                                        break;
                                    }
                                }
                            }
                            None => break,
                        }
                    }
                }
                let _ = db.db().unwatch(watch_id);
            });
            Ok(())
        });

        self.types.insert(
            model_id,
            SyncType {
                apply,
                snapshot,
                spawn_watch,
            },
        );
    }

    /// Apply a record to the local database.
    pub fn apply(&self, db: &RealmDatabase, record: &SyncRecord) -> Result<()> {
        match self.types.get(&record.model_id) {
            Some(t) => (t.apply)(db, record.op, &record.bytes),
            None => Ok(()), // unknown model — ignore
        }
    }

    /// Snapshot all records matching `filter` across the matching model(s).
    pub fn snapshot(&self, db: &RealmDatabase, filter: &SyncFilter) -> Result<Vec<SyncRecord>> {
        let mut out = Vec::new();
        for (id, t) in &self.types {
            if filter.model_id.is_some_and(|m| m != *id) {
                continue;
            }
            out.extend((t.snapshot)(db, filter.instance)?);
        }
        Ok(out)
    }

    /// Start watch tasks for every model matching `filter`, emitting changed
    /// records over `tx` until `cancel` fires.
    pub fn spawn_watch(
        &self,
        db: &RealmDatabase,
        filter: &SyncFilter,
        tx: Sender<SyncRecord>,
        cancel: CancellationToken,
    ) -> Result<()> {
        for (id, t) in &self.types {
            if filter.model_id.is_some_and(|m| m != *id) {
                continue;
            }
            (t.spawn_watch)(db, filter.instance, tx.clone(), cancel.clone())?;
        }
        Ok(())
    }
}

/// Layers register their syncable data types by submitting one of these via
/// `inventory::submit!`, mirroring the stream responder registration pattern.
///
/// ```ignore
/// inventory::submit! {
///     SyncRegistration(|r| r.register_scoped::<MyData>(|d| d._instance_id))
/// }
/// ```
pub struct SyncRegistration(pub fn(&mut SyncRegistry));
inventory::collect!(SyncRegistration);

/// The global registry of all syncable data types, assembled from every
/// `SyncRegistration` linked into the binary.
pub static SYNC: LazyLock<SyncRegistry> = LazyLock::new(|| {
    let mut registry = SyncRegistry::new();
    for registration in inventory::iter::<SyncRegistration> {
        (registration.0)(&mut registry);
    }
    registry
});

fn instance_matches<T>(
    want: Option<InstanceId>,
    instance_of: Option<fn(&T) -> InstanceId>,
    item: &T,
) -> bool {
    match (want, instance_of) {
        (None, _) => true,
        (Some(want), Some(get)) => get(item) == want,
        // Instance-scoped query against a type with no instance — no match.
        (Some(_), None) => false,
    }
}

fn event_to_record<T>(
    event: Event,
    model_id: u32,
    instance: Option<InstanceId>,
    instance_of: Option<fn(&T) -> InstanceId>,
) -> Option<SyncRecord>
where
    T: Data + Model + 'static,
{
    let (op, item): (SyncOp, T) = match event {
        Event::Insert(d) => (SyncOp::Upsert, d.inner::<T>().ok()?),
        Event::Update(d) => (SyncOp::Upsert, d.inner_new::<T>().ok()?),
        Event::Delete(d) => (SyncOp::Delete, d.inner::<T>().ok()?),
    };
    if !instance_matches(instance, instance_of, &item) {
        return None;
    }
    Some(SyncRecord {
        model_id,
        op,
        bytes: native_model::encode(&item).ok()?,
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::InstanceId;
    use crate::database::DatabaseLayer;
    use crate::realm::RealmName;
    use crate::test_db;
    use anyhow::Result;
    use native_db::ToKey;
    use native_model::Model;
    use sandpolis_macros::data;
    use std::time::Duration;

    #[data]
    #[derive(Default)]
    struct SyncTestData {
        #[secondary_key]
        _instance_id: InstanceId,
        name: String,
        value: u32,
    }

    fn record(op: SyncOp, instance: InstanceId, name: &str, value: u32) -> SyncRecord {
        let item = SyncTestData {
            _instance_id: instance,
            name: name.into(),
            value,
            ..Default::default()
        };
        SyncRecord {
            model_id: <SyncTestData as Model>::native_model_id(),
            op,
            bytes: native_model::encode(&item).unwrap(),
        }
    }

    #[tokio::test]
    async fn registry_apply_snapshot_delete() -> Result<()> {
        let mut reg = SyncRegistry::new();
        reg.register_scoped::<SyncTestData>(|d| d._instance_id);

        let db: DatabaseLayer = test_db!(SyncTestData);
        let realm = db.realm(RealmName::default())?;
        let a = InstanceId::default();
        let b = InstanceId::default();

        reg.apply(&realm, &record(SyncOp::Upsert, a, "x", 1))?;
        reg.apply(&realm, &record(SyncOp::Upsert, b, "y", 2))?;

        // Snapshot everything.
        assert_eq!(reg.snapshot(&realm, &SyncFilter::all())?.len(), 2);

        // Snapshot filtered to one instance.
        let only_a = reg.snapshot(
            &realm,
            &SyncFilter {
                model_id: None,
                instance: Some(a),
            },
        )?;
        assert_eq!(only_a.len(), 1);

        // An instance filter against the wrong model id matches nothing.
        let wrong_model = reg.snapshot(
            &realm,
            &SyncFilter {
                model_id: Some(0xDEAD),
                instance: None,
            },
        )?;
        assert_eq!(wrong_model.len(), 0);

        // Delete a's record by replaying its bytes as a Delete.
        let del = SyncRecord {
            op: SyncOp::Delete,
            ..only_a[0].clone()
        };
        reg.apply(&realm, &del)?;
        assert_eq!(reg.snapshot(&realm, &SyncFilter::all())?.len(), 1);

        Ok(())
    }

    #[tokio::test]
    async fn registry_watch_emits_changes() -> Result<()> {
        let mut reg = SyncRegistry::new();
        reg.register_scoped::<SyncTestData>(|d| d._instance_id);

        let db: DatabaseLayer = test_db!(SyncTestData);
        let realm = db.realm(RealmName::default())?;

        let (tx, mut rx) = tokio::sync::mpsc::channel(16);
        let cancel = CancellationToken::new();
        reg.spawn_watch(&realm, &SyncFilter::all(), tx, cancel.clone())?;

        // Let the watch register before mutating.
        tokio::time::sleep(Duration::from_millis(50)).await;
        reg.apply(&realm, &record(SyncOp::Upsert, InstanceId::default(), "z", 9))?;

        let got = tokio::time::timeout(Duration::from_secs(2), rx.recv())
            .await?
            .expect("watch emitted a record");
        assert_eq!(got.op, SyncOp::Upsert);

        cancel.cancel();
        Ok(())
    }
}
