pub mod smart;

pub struct DiskData {
    /// null
    pub name: String,
    /// null
    pub model: String,
    /// null
    pub serial: String,
    /// Total size in bytes
    pub size: u64,
    /// null
    pub reads: u64,
    /// null
    pub read_bytes: u64,
    /// null
    pub writes: u64,
    /// null
    pub write_bytes: u64,
    /// null
    pub queue_length: u64,
    /// null
    pub transfer_time: u64,
    /// null
    pub model_family: String,
    /// null
    pub firmware_version: String,
    /// null
    pub read_error_rate: u64,
}
