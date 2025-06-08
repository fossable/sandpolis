use native_db::*;
use native_model::{Model, native_model};
use sandpolis_core::InstanceId;
use sandpolis_database::{DataIdentifier, DbTimestamp};
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, PartialEq, Debug)]
#[native_model(id = 6, version = 1)]
#[native_db]
pub struct MemoryData {
    #[primary_key]
    pub _id: DataIdentifier,

    #[secondary_key]
    pub _instance_id: InstanceId,

    #[secondary_key]
    pub _timestamp: DbTimestamp,

    /// Handle, or instance number, associated with the structure in SMBIOS
    pub handle: String,
    /// The memory array that the device is attached to
    pub array_handle: String,
    /// Implementation form factor for this memory device
    pub form_factor: String,
    /// Total width, in bits, of this memory device, including any check or
    /// error-correction bits
    pub total_width: u32,
    /// Data width, in bits, of this memory device
    pub data_width: u32,
    /// Size of memory device in bytes
    pub size: u32,
    /// Identifies if memory device is one of a set of devices. A value of 0
    /// indicates no set affiliation.
    pub set: u32,
    /// String number of the string that identifies the physically-labeled
    /// socket or board position where the memory device is located
    pub device_location: String,
    /// String number of the string that identifies the physically-labeled bank
    /// where the memory device is located
    pub bank_location: String,
    /// Type of memory used
    pub memory_type: String,
    /// Additional details for memory device
    pub memory_type_details: String,
    /// Max speed of memory device in megatransfers per second (MT/s)
    pub max_speed: u32,
    /// Configured speed of memory device in megatransfers per second (MT/s)
    pub configured_clock_speed: u32,
    /// Manufacturer ID string
    pub manufacturer: String,
    /// Serial number of memory device
    pub serial_number: String,
    /// Manufacturer specific asset tag of memory device
    pub asset_tag: String,
    /// Manufacturer specific serial number of memory device
    pub part_number: String,
    /// Minimum operating voltage of device in millivolts
    pub min_voltage: u32,
    /// Maximum operating voltage of device in millivolts
    pub max_voltage: u32,
    /// Configured operating voltage of device in millivolts
    pub configured_voltage: u32,
}
