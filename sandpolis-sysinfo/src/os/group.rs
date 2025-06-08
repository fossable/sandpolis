use native_db::*;
use native_model::{Model, native_model};
use sandpolis_core::InstanceId;
use sandpolis_database::{Data, DataIdentifier, DbTimestamp};
use sandpolis_macros::Data;
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, Clone, PartialEq, Debug, Data)]
#[native_model(id = 2, version = 1)]
#[native_db]
pub struct GroupData {
    #[primary_key]
    pub _id: DataIdentifier,

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
