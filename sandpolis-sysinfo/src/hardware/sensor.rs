use native_db::ToKey;
use native_model::Model;
use sandpolis_core::InstanceId;
use sandpolis_macros::data;

/// A generic sensor in the system.
#[data(temporal)]
pub struct SensorData {
    #[secondary_key]
    pub _instance_id: InstanceId,

    /// A fan speed reading in RPM
    pub fan_speed: Option<u64>,
    /// A temperature reading in Celsius
    pub temperature: Option<f64>,
    /// A voltage reading in Volts
    pub voltage: Option<f64>,
}
