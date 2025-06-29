use native_db::ToKey;
use native_model::Model;
use sandpolis_core::InstanceId;
use sandpolis_database::DataIdentifier;
use sandpolis_macros::data;
use serde::{Deserialize, Serialize};

#[data(temporal)]
pub struct BatteryData {
    #[secondary_key]
    pub _instance_id: InstanceId,

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
