use native_db::ToKey;
use native_model::Model;
use sandpolis_macros::data;

#[data(instance)]
pub struct SoundDeviceData {
    /// null
    pub driver_version: String,
    /// null
    pub name: String,
    /// null
    pub codec: String,
}
