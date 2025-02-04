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

    pub group: GroupState,
    pub user: UserState,
    #[cfg(feature = "layer-sysinfo")]
    pub sysinfo: SysinfoState,
    #[cfg(feature = "layer-package")]
    pub package: PackageState,
    #[cfg(feature = "layer-shell")]
    pub shell: ShellState,
}

// TODO move to client?
/// Layers are feature-sets that may be enabled on instances.
#[derive(Serialize, Deserialize, Clone, Copy, EnumIter, Debug, PartialEq, Eq)]
pub enum Layer {
    /// Manage accounts.
    #[cfg(feature = "layer-account")]
    Account,

    #[cfg(feature = "layer-alert")]
    Alert,

    /// Deploy agents directly over a protocol like SSH or via special deployer packages.
    #[cfg(feature = "layer-deploy")]
    Deploy,

    /// Interact with Desktop environments.
    #[cfg(feature = "layer-desktop")]
    Desktop,

    /// Mount and manipulate filesystems.
    #[cfg(feature = "layer-filesystem")]
    Filesystem,

    #[cfg(feature = "layer-health")]
    Health,
    /// View system information.
    #[cfg(feature = "layer-inventory")]
    Inventory,

    #[cfg(feature = "layer-location")]
    Location,

    /// Aggregate and view logs.
    #[cfg(feature = "layer-logging")]
    Logging,

    /// Manage the Sandpolis network.
    Network,

    #[cfg(feature = "layer-package")]
    Package,

    /// Support for probe devices which do not run agent software. Instead they
    /// connect through a "gateway" instance over a well known protocol.
    #[cfg(feature = "layer-probe")]
    Probe,

    Server,

    /// Interact with shell prompts / snippets.
    #[cfg(feature = "layer-shell")]
    Shell,

    Snapshot,

    /// Establish persistent or ephemeral tunnels between instances.
    #[cfg(feature = "layer-tunnel")]
    Tunnel,
}
