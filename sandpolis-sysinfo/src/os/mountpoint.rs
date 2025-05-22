#[derive(Serialize, Deserialize, PartialEq, Debug)]
#[native_model(id = 11, version = 1)]
#[native_db]
pub struct MountpointData {
    #[primary_key]
    pub _id: u32,

    #[secondary_key]
    pub _instance_id: InstanceId,

    #[secondary_key]
    pub _timestamp: DbTimestamp,

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
