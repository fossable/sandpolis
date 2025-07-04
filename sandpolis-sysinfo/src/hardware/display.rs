use native_db::*;
use native_model::Model;
use sandpolis_core::InstanceId;
use sandpolis_macros::data;

#[data(temporal)]
pub struct DisplayData {
    #[secondary_key]
    pub _instance_id: InstanceId,

    /// The display's name
    #[secondary_key]
    pub name: String,
    /// null
    pub edid: String,
    /// The display's resolution
    pub resolution: String,
    /// The display's physical size in pixels
    pub size: String,
    /// Refresh frequency in Hertz
    pub refresh_frequency: u32,
    /// null
    pub bit_depth: u32,
}
