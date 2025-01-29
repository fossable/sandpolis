use anyhow::bail;
use anyhow::Result;
use clap::builder::OsStr;
use clap::{Parser, Subcommand};
use core::layer::server::ServerAddress;
use std::{path::PathBuf, str::FromStr};

use serde::{Deserialize, Serialize};
use strum::EnumIter;

/// Build info
pub mod built_info {
    include!(concat!(env!("OUT_DIR"), "/built.rs"));
}

fn parse_storage_dir(value: &str) -> Result<PathBuf> {
    let path = PathBuf::from_str(value)?;

    // If it's not a directory, create it
    if !std::fs::exists(&path)? {
        std::fs::create_dir_all(&path)?;
    }
    if !std::fs::metadata(&path)?.is_dir() {
        bail!("Storage directory must be a directory");
    }
    Ok(path)
}

fn default_storage_dir() -> PathBuf {
    "/tmp".into()
}

#[cfg_attr(feature = "client", derive(bevy::prelude::Resource))]
#[derive(Parser, Debug, Clone)]
#[clap(author, version, about, long_about = None)]
pub struct CommandLine {
    #[cfg(feature = "server")]
    #[clap(flatten)]
    pub server_args: crate::server::ServerCommandLine,

    #[cfg(feature = "client")]
    #[clap(flatten)]
    pub client_args: crate::client::ClientCommandLine,

    #[cfg(feature = "agent")]
    #[clap(flatten)]
    pub agent_args: crate::agent::AgentCommandLine,

    /// Servers (address:port) to connect.
    ///
    /// For GS servers, connections will be established to all given values at
    /// the same time. For LS servers, agents, and clients, only one connection
    /// can be maintained at a time.
    #[clap(long)]
    pub server: Option<Vec<ServerAddress>>,

    /// Path to authentication certificate which will be installed into the database.
    /// Subsequent runs don't require this option.
    #[clap(long)]
    pub certificate: Option<String>,

    /// Enable debug mode
    #[clap(long, num_args = 0, default_value_t = false)]
    pub debug: bool,

    /// Enable trace mode
    #[clap(long, num_args = 0, default_value_t = false)]
    pub trace: bool,

    /// Storage directory
    #[clap(long, value_parser = parse_storage_dir, default_value = default_storage_dir().into_os_string())]
    pub storage: PathBuf,

    #[command(subcommand)]
    pub command: Option<Commands>,
}

#[derive(Subcommand, Debug, Clone)]
pub enum Commands {
    #[cfg(feature = "server")]
    /// Generate a new endpoint certificate signed by the group CA
    GenerateCert {
        /// Group to generate the certificate for
        #[clap(long, default_value = "default")]
        group: String,

        /// Output file path
        #[clap(long, default_value = "./endpoint.json")]
        output: PathBuf,
    },

    InstallCert {},
}

#[cfg(feature = "agent")]
pub mod agent;

#[cfg(feature = "server")]
pub mod server;

#[cfg(feature = "client")]
pub mod client;

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
