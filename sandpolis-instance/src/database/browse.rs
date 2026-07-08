//! Type-erased database browsing.
//!
//! A [`BrowseRegistry`] knows how to, for each registered [`Data`] type:
//! - list it as a named table,
//! - snapshot its rows as generic [`serde_json::Value`]s (optionally filtered
//!   to a single instance), and
//! - enumerate the instance ids its rows belong to.
//!
//! This is the engine behind the client's generic database viewer. Every
//! `#[data]` model is registered automatically: the macro emits an
//! `inventory::submit!` of a [`BrowseRegistration`], so the [`BROWSE`] static
//! covers every model linked into the binary with zero maintenance.

use super::{Data, RealmDatabase};
use crate::InstanceId;
use anyhow::Result;
use native_model::Model;
use serde::Serialize;
use std::collections::{BTreeSet, HashMap};
use std::sync::LazyLock;

type RowsFn = Box<
    dyn Fn(&RealmDatabase, Option<InstanceId>) -> Result<Vec<serde_json::Value>> + Send + Sync,
>;
type InstancesFn = Box<dyn Fn(&RealmDatabase) -> Result<Vec<InstanceId>> + Send + Sync>;

struct BrowseType {
    /// Struct name with the module path stripped.
    name: &'static str,
    rows: RowsFn,
    /// `None` for types with no owning instance.
    instances: Option<InstancesFn>,
}

#[derive(Default)]
pub struct BrowseRegistry {
    types: HashMap<u32, BrowseType>,
}

impl BrowseRegistry {
    pub fn new() -> Self {
        Self::default()
    }

    /// Register a type with no instance scoping. Instance-filtered queries
    /// never match it.
    pub fn register<T>(&mut self)
    where
        T: Data + Model + Serialize + 'static,
    {
        self.register_inner::<T>(None);
    }

    /// Register a type whose records belong to an instance, extracted by
    /// `instance_of` (typically `|d| d._instance_id`).
    pub fn register_scoped<T>(&mut self, instance_of: fn(&T) -> InstanceId)
    where
        T: Data + Model + Serialize + 'static,
    {
        self.register_inner::<T>(Some(instance_of));
    }

    fn register_inner<T>(&mut self, instance_of: Option<fn(&T) -> InstanceId>)
    where
        T: Data + Model + Serialize + 'static,
    {
        let model_id = T::native_model_id();
        let name = std::any::type_name::<T>()
            .rsplit("::")
            .next()
            .expect("type name is never empty");

        let rows: RowsFn = Box::new(move |db, instance| {
            let r = db.r_transaction()?;
            let items: Vec<T> = r
                .scan()
                .primary::<T>()?
                .all()?
                .collect::<std::result::Result<Vec<_>, _>>()?;
            drop(r);

            let mut out = Vec::new();
            for item in items {
                match (instance, instance_of) {
                    (Some(want), Some(get)) if get(&item) != want => continue,
                    // Instance-scoped query against a type with no instance.
                    (Some(_), None) => continue,
                    _ => {}
                }
                out.push(serde_json::to_value(&item)?);
            }
            Ok(out)
        });

        let instances: Option<InstancesFn> = instance_of.map(|get| -> InstancesFn {
            Box::new(move |db| {
                let r = db.r_transaction()?;
                let items: Vec<T> = r
                    .scan()
                    .primary::<T>()?
                    .all()?
                    .collect::<std::result::Result<Vec<_>, _>>()?;
                drop(r);
                let set: BTreeSet<InstanceId> = items.iter().map(get).collect();
                Ok(set.into_iter().collect())
            })
        });

        self.types.insert(
            model_id,
            BrowseType {
                name,
                rows,
                instances,
            },
        );
    }

    /// All registered tables as `(model_id, name)`, sorted by name then id.
    /// Short names can collide across crates (e.g. two `UserData`s); the
    /// model id disambiguates selection.
    pub fn tables(&self) -> Vec<(u32, &'static str)> {
        let mut out: Vec<_> = self.types.iter().map(|(id, t)| (*id, t.name)).collect();
        out.sort_by(|a, b| a.1.cmp(b.1).then(a.0.cmp(&b.0)));
        out
    }

    /// Snapshot the rows of one table, optionally filtered to one instance.
    pub fn rows(
        &self,
        db: &RealmDatabase,
        model_id: u32,
        instance: Option<InstanceId>,
    ) -> Result<Vec<serde_json::Value>> {
        match self.types.get(&model_id) {
            Some(t) => (t.rows)(db, instance),
            None => Ok(Vec::new()),
        }
    }

    /// Row count for one table (full scan; fine for a debug tool).
    pub fn count(
        &self,
        db: &RealmDatabase,
        model_id: u32,
        instance: Option<InstanceId>,
    ) -> Result<usize> {
        Ok(self.rows(db, model_id, instance)?.len())
    }

    /// Distinct instance ids across every instance-scoped table (drives the
    /// viewer's instance filter).
    pub fn instances(&self, db: &RealmDatabase) -> Vec<InstanceId> {
        let mut set = BTreeSet::new();
        for t in self.types.values() {
            if let Some(f) = &t.instances {
                // Tables not defined in the database's models scan-error; skip.
                if let Ok(ids) = f(db) {
                    set.extend(ids);
                }
            }
        }
        set.into_iter().collect()
    }
}

/// Submitted automatically by the `#[data]` attribute macro for every model,
/// mirroring `SyncRegistration`.
pub struct BrowseRegistration(pub fn(&mut BrowseRegistry));
inventory::collect!(BrowseRegistration);

/// The global registry of all browsable data types linked into the binary.
pub static BROWSE: LazyLock<BrowseRegistry> = LazyLock::new(|| {
    let mut registry = BrowseRegistry::new();
    for registration in inventory::iter::<BrowseRegistration> {
        (registration.0)(&mut registry);
    }
    registry
});

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

    #[data]
    #[derive(Default)]
    struct BrowseTestData {
        #[secondary_key]
        _instance_id: InstanceId,
        name: String,
        value: u32,
    }

    #[tokio::test]
    async fn registry_tables_rows_instances() -> Result<()> {
        let mut reg = BrowseRegistry::new();
        reg.register_scoped::<BrowseTestData>(|d| d._instance_id);

        let db: DatabaseLayer = test_db!(BrowseTestData);
        let realm = db.realm(RealmName::default())?;
        let a = InstanceId::default();
        let b = InstanceId::default();

        let rw = realm.rw_transaction()?;
        rw.insert(BrowseTestData {
            _instance_id: a,
            name: "x".into(),
            value: 1,
            ..Default::default()
        })?;
        rw.insert(BrowseTestData {
            _instance_id: b,
            name: "y".into(),
            value: 2,
            ..Default::default()
        })?;
        rw.commit()?;

        let model_id = <BrowseTestData as Model>::native_model_id();

        // The table is listed under its stripped name.
        assert_eq!(reg.tables(), vec![(model_id, "BrowseTestData")]);

        // Rows serialize generically and filter by instance.
        let all = reg.rows(&realm, model_id, None)?;
        assert_eq!(all.len(), 2);
        assert!(all[0].get("name").is_some());
        assert_eq!(reg.rows(&realm, model_id, Some(a))?.len(), 1);
        assert_eq!(reg.count(&realm, model_id, Some(b))?, 1);

        // Unknown model ids match nothing.
        assert_eq!(reg.rows(&realm, 0xDEAD, None)?.len(), 0);

        // Both instances are discovered.
        assert_eq!(reg.instances(&realm), {
            let mut ids = vec![a, b];
            ids.sort();
            ids
        });

        Ok(())
    }

    #[tokio::test]
    async fn global_registry_populated_by_macro() -> Result<()> {
        // The `#[data]` macro auto-submits a registration for every model in
        // this crate (including `BrowseTestData` above), so the global
        // registry must contain it.
        assert!(
            BROWSE
                .tables()
                .iter()
                .any(|(_, name)| *name == "BrowseTestData")
        );
        Ok(())
    }
}
