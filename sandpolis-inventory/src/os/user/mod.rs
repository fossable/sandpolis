use native_db::ToKey;
use native_model::Model;
use sandpolis_instance::InstanceId;
use sandpolis_macros::data;

#[cfg(all(feature = "agent", not(feature = "uki")))]
pub mod agent;

/// Information about an "operating-system" level user account.
#[data(defaults)]
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

inventory::submit! {
    sandpolis_instance::database::sync::SyncRegistration(|r| {
        r.register_scoped::<UserData>(|d| d._instance_id)
    })
}
