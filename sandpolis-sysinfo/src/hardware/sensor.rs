/// A generic sensor in the system.
pub struct SensorData {
    /// A fan speed reading in RPM
    pub fan_speed: Option<u64>,
    /// A temperature reading in Celsius
    pub temperature: Option<f64>,
    /// A voltage reading in Volts
    pub voltage: Option<f64>,
}
