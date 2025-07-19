#[data(instance)]
pub struct BlockDeviceData {
    /// Block device name
    pub name: String,
    /// Block device parent name
    pub parent: String,
    /// Block device vendor string
    pub vendor: Option<String>,
    /// Block device model string identifier
    pub model: String,
    /// Block device size in blocks
    pub size: u64,
    /// Block size in bytes
    pub block_size: u64,
    /// Block device Universally Unique Identifier
    pub uuid: String,
    /// Block device type string
    pub r#type: String,
    /// Block device label string
    pub label: String,
}
