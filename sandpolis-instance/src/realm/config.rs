use crate::realm::RealmName;
use serde::{Deserialize, Serialize};
use std::path::PathBuf;

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct RealmConfig {
    /// Path to realm certificate which will be installed into the
    /// database.
    #[cfg(feature = "agent")]
    pub agent_certs: Option<Vec<PathBuf>>,

    /// Path to realm certificate which will be installed into the
    /// database.
    #[cfg(feature = "client")]
    pub client_certs: Option<Vec<PathBuf>>,

    /// Force the following realms to exist
    pub static_realms: Option<Vec<RealmName>>,

    /// Whether new realms can be created
    pub lock_realms: Option<bool>,
}
