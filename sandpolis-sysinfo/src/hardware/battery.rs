use native_db::*;
use native_model::{Model, native_model};
use sandpolis_core::InstanceId;
use sandpolis_database::{DataIdentifier, DbTimestamp};
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, PartialEq, Debug)]
#[native_model(id = 7, version = 1)]
#[native_db]
pub struct BatteryData {
    #[primary_key]
    pub _id: DataIdentifier,

    #[secondary_key]
    pub _instance_id: InstanceId,

    #[secondary_key]
    pub _timestamp: DbTimestamp,

    /// Manufacturer's name
    pub manufacturer: Option<String>,

    /// The date the battery was manufactured UNIX Epoch
    pub manufacture_date: Option<u64>,

    /// Model number
    pub model: Option<String>,

    /// Serial number
    pub serial_number: Option<String>,

    /// Number of charge/discharge cycles
    pub cycle_count: Option<u64>,

    /// Whether the battery is currently being changed by a power source
    pub charging: Option<bool>,

    /// Whether the battery is completely charged
    pub charged: Option<bool>,

    /// Maximum capacity specification in mAh
    pub specified_capacity: Option<u32>,

    /// Actual maximum capacity in mAh
    pub actual_capacity: Option<u32>,

    /// Current capacity in mAh
    pub remaining_capacity: Option<u32>,

    /// Amperage in mA
    pub current: Option<u32>,

    /// Voltage in mV
    pub voltage: Option<u32>,
}
