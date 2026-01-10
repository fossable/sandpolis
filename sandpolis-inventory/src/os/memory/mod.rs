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

impl Default for MemoryData {
    fn default() -> Self {
        Self {
            total: 0,
            free: 0,
            file_buffers: 0,
            cached: 0,
            swap_cached: 0,
            active: 0,
            inactive: 0,
            swap_total: 0,
            swap_free: 0,
            _id: Default::default(),
            _revision: Default::default(),
            _creation: Default::default(),
        }
    }
}
