#[derive(Serialize, Deserialize, PartialEq, Debug)]
#[native_model(id = 10, version = 1)]
#[native_db]
pub struct MainboardData {
    #[primary_key]
    pub _id: u32,

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
