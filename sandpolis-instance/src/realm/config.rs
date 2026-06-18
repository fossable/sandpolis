use crate::realm::RealmName;
use serde::{Deserialize, Serialize};
use std::path::PathBuf;

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct RealmConfig {
    /// Paths to realm certs supplied via `--realm-cert`. These are loaded fresh
    /// on every run and never persisted, so this field is not part of the
    /// on-disk config.
    #[serde(skip)]
    pub realm_certs: Vec<PathBuf>,

    /// Force the following realms to exist
    pub static_realms: Option<Vec<RealmName>>,

    /// Whether new realms can be created
    pub lock_realms: Option<bool>,
}
