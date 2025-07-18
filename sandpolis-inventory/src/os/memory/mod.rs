use native_db::ToKey;
use native_model::Model;
use sandpolis_macros::data;

#[cfg(feature = "agent")]
pub mod agent;

// TODO id conflict
#[data(id = 1000)]
pub struct MemoryData {
    /// The amount of physical RAM in bytes
    pub total: u64,
    /// The amount of physical RAM, in bytes, left unused by the system
    pub free: u64,
    /// The amount of physical RAM, in bytes, used for file buffers
    pub file_buffers: u64,
    /// The amount of physical RAM, in bytes, used as cache memory
    pub cached: u64,
    /// The amount of sawp, in bytes, used as cache memory
    pub swap_cached: u64,
    /// The total amount of buffer or page cache memory, in bytes, that is in
    /// active use
    pub active: u64,
    /// The total amount of buffer or page cache memory, in bytes, that are free
    /// and available
    pub inactive: u64,
    /// The total amount of swap available, in bytes
    pub swap_total: u64,
    /// The total amount of swap free, in bytes
    pub swap_free: u64,
}
