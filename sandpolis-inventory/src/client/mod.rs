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

#[cfg(feature = "client")]
pub mod gui;

/// Subscribe to live inventory updates for an instance (call when a view opens).
pub fn subscribe(instance: InstanceId) {
    sandpolis_client::sync::subscribe_all(inventory_model_ids(), Some(instance));
}

/// Unsubscribe from inventory updates for an instance (call when a view closes).
pub fn unsubscribe(instance: InstanceId) {
    sandpolis_client::sync::unsubscribe_all(inventory_model_ids(), Some(instance));
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

/// Query the live memory usage for an instance.
pub fn query_memory(id: InstanceId) -> anyhow::Result<Option<MemoryData>> {
    Ok(sandpolis_client::sync::scan_all::<MemoryData>()?
        .into_iter()
        .find(|m| m._instance_id == id))
}

/// Query the live per-core CPU utilization for an instance, ordered by core.
pub fn query_cpu_cores(id: InstanceId) -> anyhow::Result<Vec<CpuCoreData>> {
    let mut cores: Vec<CpuCoreData> = sandpolis_client::sync::scan_all::<CpuCoreData>()?
        .into_iter()
        .filter(|core| core._instance_id == id)
        .collect();
    cores.sort_by_key(|core| core.index);
    Ok(cores)
}

/// Query the mounted filesystems known for an instance, largest first.
pub fn query_mountpoints(id: InstanceId) -> anyhow::Result<Vec<MountpointData>> {
    let mut mounts: Vec<MountpointData> = sandpolis_client::sync::scan_all::<MountpointData>()?
        .into_iter()
        .filter(|mount| mount._instance_id == id && mount.mounted && mount.total_bytes() > 0)
        .collect();
    mounts.sort_by_key(|mount| std::cmp::Reverse(mount.total_bytes()));
    Ok(mounts)
}

/// Query the user accounts known for an instance.
pub fn query_users(id: InstanceId) -> anyhow::Result<Vec<UserData>> {
    Ok(sandpolis_client::sync::scan_all::<UserData>()?
        .into_iter()
        .filter(|u| u._instance_id == id)
        .collect())
}

/// Query the installed packages known for an instance.
pub fn query_packages(id: InstanceId) -> anyhow::Result<Vec<PackageData>> {
    Ok(sandpolis_client::sync::scan_all::<PackageData>()?
        .into_iter()
        .filter(|p| p._instance_id == id)
        .collect())
}
