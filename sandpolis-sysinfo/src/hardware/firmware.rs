#[derive(Serialize, Deserialize, PartialEq, Debug)]
#[native_model(id = 9, version = 1)]
#[native_db]
pub struct FirmwareData {
    #[primary_key]
    pub _id: u32,

    #[secondary_key]
    pub _instance_id: InstanceId,

    #[secondary_key]
    pub _timestamp: DbTimestamp,

    /// null
    pub name: String,
    /// The BIOS manufacturer title
    pub manufacturer: String,
    /// The BIOS description
    pub description: String,
    /// The BIOS version number
    pub version: String,
    /// The BIOS revision number
    pub revision: String,
    /// The BIOS release date
    pub release_date: String,
    /// Whether the BIOS supports UEFI mode
    pub uefi: bool,
}
