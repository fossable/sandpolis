//! Client-side access to synced inventory data.
//!
//! Mirrors the health layer: a view subscribes to the relevant models when it
//! opens, and reads the records the sync layer has replicated into the client's
//! local database.

use crate::hardware::cpu::CpuCoreData;
use crate::os::memory::MemoryData;
use crate::os::mountpoint::MountpointData;
use crate::os::user::UserData;
use crate::package::PackageData;
use native_model::Model;
use sandpolis_instance::InstanceId;
use sandpolis_instance::database::Data;
use sandpolis_instance::realm::RealmName;

#[cfg(feature = "client")]
pub mod gui;

/// Subscribe to live inventory updates for an instance (call when a view opens).
pub fn subscribe(instance: InstanceId) {
    for model_id in inventory_model_ids() {
        sandpolis_client::sync::subscribe(model_id, Some(instance));
    }
}

/// Unsubscribe from inventory updates for an instance (call when a view closes).
pub fn unsubscribe(instance: InstanceId) {
    for model_id in inventory_model_ids() {
        sandpolis_client::sync::unsubscribe(model_id, Some(instance));
    }
}

fn inventory_model_ids() -> [u32; 5] {
    [
        <MemoryData as Model>::native_model_id(),
        <CpuCoreData as Model>::native_model_id(),
        <MountpointData as Model>::native_model_id(),
        <UserData as Model>::native_model_id(),
        <PackageData as Model>::native_model_id(),
    ]
}

/// Read all records of a model from the client's local (synced) database.
fn all<T: Data>() -> anyhow::Result<Vec<T>> {
    let Some(database) = sandpolis_client::sync::client_database() else {
        return Ok(vec![]);
    };
    let realm = database.realm(RealmName::default())?;
    let r = realm.r_transaction()?;
    Ok(r.scan()
        .primary::<T>()?
        .all()?
        .collect::<std::result::Result<Vec<_>, _>>()?)
}

/// Query the live memory usage for an instance.
pub fn query_memory(id: InstanceId) -> anyhow::Result<Option<MemoryData>> {
    Ok(all::<MemoryData>()?
        .into_iter()
        .find(|m| m._instance_id == id))
}

/// Query the live per-core CPU utilization for an instance, ordered by core.
pub fn query_cpu_cores(id: InstanceId) -> anyhow::Result<Vec<CpuCoreData>> {
    let mut cores: Vec<CpuCoreData> = all::<CpuCoreData>()?
        .into_iter()
        .filter(|core| core._instance_id == id)
        .collect();
    cores.sort_by_key(|core| core.index);
    Ok(cores)
}

/// Query the mounted filesystems known for an instance, largest first.
pub fn query_mountpoints(id: InstanceId) -> anyhow::Result<Vec<MountpointData>> {
    let mut mounts: Vec<MountpointData> = all::<MountpointData>()?
        .into_iter()
        .filter(|mount| mount._instance_id == id && mount.mounted && mount.total_bytes() > 0)
        .collect();
    mounts.sort_by_key(|mount| std::cmp::Reverse(mount.total_bytes()));
    Ok(mounts)
}

/// Query the user accounts known for an instance.
pub fn query_users(id: InstanceId) -> anyhow::Result<Vec<UserData>> {
    Ok(all::<UserData>()?
        .into_iter()
        .filter(|u| u._instance_id == id)
        .collect())
}

/// Query the installed packages known for an instance.
pub fn query_packages(id: InstanceId) -> anyhow::Result<Vec<PackageData>> {
    Ok(all::<PackageData>()?
        .into_iter()
        .filter(|p| p._instance_id == id)
        .collect())
}
