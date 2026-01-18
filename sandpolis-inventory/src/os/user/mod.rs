use native_db::ToKey;
use native_model::Model;
use sandpolis_core::InstanceId;
use sandpolis_macros::data;

#[cfg(feature = "agent")]
pub mod agent;

/// Information about an "operating-system" level user account.
#[data]
#[derive(Default)]
pub struct UserData {
    #[secondary_key]
    pub _instance_id: InstanceId,

    /// Description
    pub description: Option<String>,
    /// Home directory
    pub directory: Option<String>,
    /// Group ID
    pub gid: u32,
    /// The user's default shell
    pub shell: Option<String>,
    /// User ID
    #[secondary_key]
    pub uid: u32,
    /// Username
    #[secondary_key]
    pub username: Option<String>,
}
