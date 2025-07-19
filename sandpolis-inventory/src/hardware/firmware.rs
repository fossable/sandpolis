use native_db::*;
use native_model::Model;
use sandpolis_macros::data;

#[data(instance)]
pub struct FirmwareData {
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
