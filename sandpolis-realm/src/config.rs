use sandpolis_core::RealmName;
use serde::{Deserialize, Serialize};
use std::path::PathBuf;

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct RealmConfig {
    /// Path to authentication certificate which will be installed into the
    /// database. Subsequent runs don't require this option.
    pub certificate: Option<PathBuf>,

    /// Force the following realms to exist
    pub static_realms: Option<Vec<RealmName>>,

    /// Whether new realms can be created
    pub lock_realms: Option<bool>,
}
