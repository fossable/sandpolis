use native_db::ToKey;
use native_model::Model;
use sandpolis_macros::data;

#[data(instance)]
pub struct CpuData {
    /// Product model
    pub model: Option<String>,
    /// Vendor name
    pub vendor: Option<String>,
    /// The size of the L1 cache in bytes
    pub l1_cache: Option<u64>,
    /// The size of the L2 cache in bytes
    pub l2_cache: Option<u64>,
    /// The size of the L3 cache in bytes
    pub l3_cache: Option<u64>,
    /// The size of the L4 cache in bytes
    pub l4_cache: Option<u64>,
}

#[data(instance)]
pub struct CpuCoreData {
    #[secondary_key]
    pub index: u32,
    /// The specified frequency in Hertz
    pub frequency_spec: u64,
    /// The core's actual frequency in Hertz
    pub frequency: u64,
    /// The core's utilization between 0.0 and 1.0
    pub usage: f64,
    /// The core's temperature in Celsius
    pub temperature: Option<f64>,
}
