use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, Clone, Default)]
pub struct UserData {
    /// User ID
    pub uid: u64,
    /// Group ID
    pub gid: u64,
    /// Username
    pub username: Option<String>,
    /// Description
    pub description: Option<String>,
    /// Home directory
    pub directory: Option<String>,
    /// The user's default shell
    pub shell: Option<String>,
}
