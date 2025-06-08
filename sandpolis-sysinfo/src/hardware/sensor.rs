use native_db::*;
use native_model::{Model, native_model};
use sandpolis_core::InstanceId;
use sandpolis_database::{DataIdentifier, DbTimestamp};
use serde::{Deserialize, Serialize};

/// A generic sensor in the system.
#[derive(Serialize, Deserialize, PartialEq, Debug)]
#[native_model(id = 7, version = 1)]
#[native_db]
pub struct SensorData {
    #[primary_key]
    pub _id: DataIdentifier,

    #[secondary_key]
    pub _instance_id: InstanceId,

    #[secondary_key]
    pub _timestamp: DbTimestamp,

    /// A fan speed reading in RPM
    pub fan_speed: Option<u64>,
    /// A temperature reading in Celsius
    pub temperature: Option<f64>,
    /// A voltage reading in Volts
    pub voltage: Option<f64>,
}
