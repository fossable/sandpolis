use anyhow::bail;
use anyhow::Result;
use axum::extract::FromRef;
use sandpolis_network::ServerAddress;
use serde::{Deserialize, Serialize};
use std::{path::PathBuf, str::FromStr};
use strum::EnumIter;

/// Build info
pub mod built_info {
    include!(concat!(env!("OUT_DIR"), "/built.rs"));
}

#[derive(Clone, FromRef)]
pub struct State {
    pub db: Database,

    pub group: GroupLayer,
    #[cfg(feature = "layer-package")]
    pub package: PackageLayer,
    #[cfg(feature = "layer-shell")]
    pub power: PowerLayer,
    #[cfg(feature = "layer-shell")]
    pub shell: ShellLayer,
    #[cfg(feature = "layer-sysinfo")]
    pub sysinfo: SysinfoLayer,
    pub user: UserLayer,
}
