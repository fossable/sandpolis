use native_db::ToKey;
use native_model::Model;
use sandpolis_core::InstanceId;
use sandpolis_macros::data;

#[data]
pub struct GroupData {
    #[secondary_key]
    pub _instance_id: InstanceId,

    /// Unsigned int64 group ID
    pub gid: u64,
    /// Canonical local group name
    pub name: String,
    /// Unique group ID
    pub group_sid: Option<String>,
    /// Remarks or comments associated with the group
    pub comment: Option<String>,
}
