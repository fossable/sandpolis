use native_db::*;
use native_model::{Model, native_model};
use sandpolis_core::InstanceId;
use sandpolis_database::{Data, DataIdentifier, DbTimestamp};
use sandpolis_macros::Data;
use serde::{Deserialize, Serialize};

#[cfg(feature = "agent")]
pub mod agent;

/// Information about an "operating-system" level user account.
#[derive(Serialize, Deserialize, Clone, PartialEq, Debug, Data)]
#[native_model(id = 4, version = 1)]
#[native_db]
pub struct UserData {
    #[primary_key]
    pub _id: DataIdentifier,

    #[secondary_key]
    pub _instance_id: InstanceId,

    #[secondary_key]
    pub _timestamp: DbTimestamp,

    /// Description
    pub description: Option<String>,
    /// Home directory
    pub directory: Option<String>,
    /// Group ID
    pub gid: u64,
    /// The user's default shell
    pub shell: Option<String>,
    /// User ID
    #[secondary_key]
    pub uid: u64,
    /// Username
    #[secondary_key]
    pub username: Option<String>,
}
