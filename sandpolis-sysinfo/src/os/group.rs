#[derive(Serialize, Deserialize, PartialEq, Debug)]
#[native_model(id = 2, version = 1)]
#[native_db]
pub struct GroupData {
    #[primary_key]
    pub id: u32,

    #[secondary_key]
    pub instance_id: InstanceId,

    /// Unsigned int64 group ID
    pub gid: u64,
    /// Canonical local group name
    pub name: String,
    /// Unique group ID
    pub group_sid: Option<String>,
    /// Remarks or comments associated with the group
    pub comment: Option<String>,
}
