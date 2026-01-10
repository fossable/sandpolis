use native_db::ToKey;
use native_model::Model;
use sandpolis_core::InstanceId;
use sandpolis_macros::data;

#[cfg(feature = "agent")]
pub mod agent;

/// Information about an "operating-system" level user account.
#[data]
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

impl Default for UserData {
    fn default() -> Self {
        Self {
            _instance_id: Default::default(),
            description: None,
            directory: None,
            gid: 0,
            shell: None,
            uid: 0,
            username: None,
            _id: Default::default(),
            _revision: Default::default(),
            _creation: Default::default(),
        }
    }
}
