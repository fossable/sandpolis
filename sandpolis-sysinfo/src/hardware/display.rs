#[derive(Serialize, Deserialize, PartialEq, Debug)]
#[native_model(id = 8, version = 1)]
#[native_db]
pub struct DisplayData {
    #[primary_key]
    pub _id: u32,

    #[secondary_key]
    pub _instance_id: InstanceId,

    #[secondary_key]
    pub _timestamp: DbTimestamp,

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
