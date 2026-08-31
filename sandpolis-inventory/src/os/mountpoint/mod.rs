use native_db::ToKey;
use native_model::Model;
use sandpolis_instance::InstanceId;
use sandpolis_macros::data;

#[cfg(all(feature = "agent", not(feature = "uki")))]
pub mod agent;

#[data(temporal)]
#[derive(Default)]
pub struct MountpointData {
    #[secondary_key]
    pub _instance_id: InstanceId,

    /// Whether the mountpoint is actually mounted. This could be false for
    /// unmounted /etc/fstab entries for example.
    pub mounted: bool,
    /// Mounted device
    pub device: String,
    /// Mounted device alias
    pub device_alias: String,
    /// Mounted device path
    pub path: String,
    /// Mounted device type
    pub r#type: String,
    /// Block size in bytes
    pub blocks_size: u64,
    /// Mounted device used blocks
    pub blocks: u64,
    /// Mounted device free blocks
    pub blocks_free: u64,
    /// Mounted device available blocks
    pub blocks_available: u64,
    /// Mounted device used inodes
    pub inodes: u64,
    /// Mounted device free inodes
    pub inodes_free: u64,
    /// Mounted device flags
    pub flags: String,
}

impl MountpointData {
    /// Total capacity in bytes.
    pub fn total_bytes(&self) -> u64 {
        self.blocks.saturating_mul(self.blocks_size)
    }

    /// Used capacity in bytes.
    ///
    /// Derived from the *available* count rather than the free one, so it
    /// matches what `df` reports: the blocks reserved for root are neither free
    /// to the user nor usefully counted as in use by anything.
    pub fn used_bytes(&self) -> u64 {
        self.blocks
            .saturating_sub(self.blocks_available)
            .saturating_mul(self.blocks_size)
    }
}

inventory::submit! {
    sandpolis_instance::database::sync::SyncRegistration(|r| {
        r.register_scoped::<MountpointData>(|d| d._instance_id)
    })
}
