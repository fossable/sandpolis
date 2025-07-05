use native_db::ToKey;
use native_model::Model;
use sandpolis_core::InstanceId;
use sandpolis_macros::data;

#[data]
pub struct MainboardData {
    #[secondary_key]
    pub _instance_id: InstanceId,

    /// null
    pub model: String,
    /// null
    pub manufacturer: String,
    /// null
    pub version: String,
    /// null
    pub serial_number: String,
}
