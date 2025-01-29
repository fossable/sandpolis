pub struct MountpointData {
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
