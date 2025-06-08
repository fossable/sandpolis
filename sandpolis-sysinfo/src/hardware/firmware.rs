use native_db::*;
use native_model::{Model, native_model};
use sandpolis_core::InstanceId;
use sandpolis_database::{DataIdentifier, DbTimestamp};
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, PartialEq, Debug)]
#[native_model(id = 9, version = 1)]
#[native_db]
pub struct FirmwareData {
    #[primary_key]
    pub _id: DataIdentifier,

    #[secondary_key]
    pub _instance_id: InstanceId,

    #[secondary_key]
    pub _timestamp: DbTimestamp,

    /// null
    pub name: String,
    /// Firmware manufacturer title
    pub manufacturer: String,
    /// Firmware description
    pub description: String,
    /// Firmware version number
    pub version: String,
    /// Firmware revision number
    pub revision: String,
    /// Firmware release date
    pub release_date: String,
    /// Whether the firmware supports UEFI mode
    pub uefi: bool,
}
