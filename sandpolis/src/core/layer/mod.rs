use serde::{Deserialize, Serialize};
use strum::EnumIter;

#[cfg(feature = "layer-desktop")]
pub mod desktop;
#[cfg(feature = "layer-filesystem")]
pub mod filesystem;
#[cfg(feature = "layer-location")]
pub mod location;
#[cfg(feature = "layer-package")]
pub mod package;
#[cfg(feature = "layer-probe")]
pub mod probe;
#[cfg(feature = "layer-shell")]
pub mod shell;
#[cfg(feature = "layer-sysinfo")]
pub mod sysinfo;
#[cfg(feature = "layer-tunnel")]
pub mod tunnel;

// These layers are required
pub mod agent;
pub mod network;
pub mod server;

/// Layers are feature-sets that may be enabled on instances.
#[derive(Serialize, Deserialize, Clone, Copy, EnumIter, Debug, PartialEq, Eq)]
pub enum Layer {
    /// Manage accounts.
    #[cfg(feature = "layer-account")]
    Account,

    #[cfg(feature = "layer-alerts")]
    Alerts,

    /// Deploy agents directly over a protocol like SSH or via special deployer packages.
    #[cfg(feature = "layer-deploy")]
    Deploy,

    /// Interact with Desktop environments.
    #[cfg(feature = "layer-desktop")]
    Desktop,

    /// Mount and manipulate filesystems.
    #[cfg(feature = "layer-filesystem")]
    Filesystem,

    // Goldboot,
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

    /// Establish persistent or ephemeral tunnels between instances.
    #[cfg(feature = "layer-tunnel")]
    Tunnel,
}
