use native_db::*;
use native_model::{Model, native_model};
use sandpolis_core::InstanceId;
use sandpolis_database::{DataIdentifier, DbTimestamp};
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, PartialEq, Debug)]
#[native_model(id = 10, version = 1)]
#[native_db]
pub struct MainboardData {
    #[primary_key]
    pub _id: DataIdentifier,

    #[secondary_key]
    pub _instance_id: InstanceId,

    #[secondary_key]
    pub _timestamp: DbTimestamp,

    /// null
    pub model: String,
    /// null
    pub manufacturer: String,
    /// null
    pub version: String,
    /// null
    pub serial_number: String,
}
