pub struct CpuData {
    /// null
    pub model: Option<String>,
    /// null
    pub vendor: Option<String>,
    /// The specified frequency in Hertz
    pub frequency_spec: u64,
    /// The size of the L1 cache in bytes
    pub l1_cache: u64,
    /// The size of the L2 cache in bytes
    pub l2_cache: u64,
    /// The size of the L3 cache in bytes
    pub l3_cache: u64,
    /// The size of the L4 cache in bytes
    pub l4_cache: u64,
}

pub struct CpuCoreData {
    /// The core's usage between 0.0 and 1.0
    pub usage: Double,
    /// The core's temperature in Celsius
    pub temperature: Double,
}
