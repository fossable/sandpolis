/// null
pub model: java.lang.String,
/// null
pub vendor: java.lang.String,
/// The specified frequency in Hertz
pub frequency_spec: java.lang.Integer,
/// The size of the L1 cache in bytes
pub l1_cache: java.lang.Integer,
/// The size of the L2 cache in bytes
pub l2_cache: java.lang.Integer,
/// The size of the L3 cache in bytes
pub l3_cache: java.lang.Integer,
/// The size of the L4 cache in bytes
pub l4_cache: java.lang.Integer,

pub struct Core {
/// The core's usage between 0.0 and 1.0
pub usage: java.lang.Double,
/// The core's temperature in Celsius
pub temperature: java.lang.Double,
}
