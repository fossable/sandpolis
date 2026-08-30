use native_db::ToKey;
use native_model::Model;
use sandpolis_macros::data;

#[cfg(feature = "agent")]
pub mod agent;

#[data(instance)]
#[derive(Default)]
pub struct PartitionData {
    /// Path of the partition's device node (e.g. `/dev/sda1`)
    pub identification: String,
    /// Kernel name of the partition (e.g. `sda1`)
    pub name: String,
    /// Kernel name of the parent disk (e.g. `sda`)
    pub description: String,
    /// The partition's UUID
    #[secondary_key]
    pub uuid: String,
    /// The partition's total size in bytes
    pub size: u64,
    /// Device major number
    pub major: u32,
    /// Device minor number
    pub minor: u32,
    /// The partition's mount point
    pub mount: String,
}

inventory::submit! {
    sandpolis_instance::database::sync::SyncRegistration(|r| {
        r.register_scoped::<PartitionData>(|d| d._instance_id)
    })
}
