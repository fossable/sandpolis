use std::collections::HashMap;

use anyhow::Result;
use axum_macros::FromRef;
use config::Configuration;
use sandpolis_database::Database;
use sandpolis_instance::LayerVersion;
use serde::{Deserialize, Serialize};
use strum::EnumIter;

pub mod cli;
pub mod config;
pub mod routes;

#[cfg(feature = "server")]
pub mod server;

#[cfg(feature = "agent")]
pub mod agent;

#[cfg(feature = "client")]
pub mod client;

/// Build info
pub mod built_info {
    include!(concat!(env!("OUT_DIR"), "/built.rs"));
}

#[derive(Clone, FromRef)]
pub struct InstanceState {
    pub agent: sandpolis_agent::AgentLayer,
    #[cfg(feature = "layer-filesystem")]
    pub filesystem: sandpolis_filesystem::FilesystemLayer,
    pub group: sandpolis_group::GroupLayer,
    pub instance: sandpolis_instance::InstanceLayer,
    pub network: sandpolis_network::NetworkLayer,
    #[cfg(feature = "layer-package")]
    pub package: sandpolis_package::PackageLayer,
    #[cfg(feature = "layer-power")]
    pub power: sandpolis_power::PowerLayer,
    pub server: sandpolis_server::ServerLayer,
    #[cfg(feature = "layer-shell")]
    pub shell: sandpolis_shell::ShellLayer,
    #[cfg(feature = "layer-sysinfo")]
    pub sysinfo: sandpolis_sysinfo::SysinfoLayer,
    pub user: sandpolis_user::UserLayer,
}

impl InstanceState {
    pub async fn new(config: Configuration, db: Database) -> Result<Self> {
        let instance = sandpolis_instance::InstanceLayer::new(db.document("/instance")?)?;
        let group = sandpolis_group::GroupLayer::new(
            config.group,
            db.document("/group")?,
            instance.clone(),
        )?;
        let network = sandpolis_network::NetworkLayer::new(
            config.network,
            db.document("/network")?,
            group.clone(),
        )?;
        Ok(Self {
            #[cfg(feature = "layer-package")]
            package: sandpolis_package::PackageLayer::new()?,
            #[cfg(feature = "layer-power")]
            power: sandpolis_power::PowerLayer {
                network: network.clone(),
            },
            #[cfg(feature = "layer-shell")]
            shell: sandpolis_shell::ShellLayer::new()?,
            #[cfg(feature = "layer-sysinfo")]
            sysinfo: sandpolis_sysinfo::SysinfoLayer::new(db.document("/sysinfo")?)?,
            #[cfg(feature = "layer-filesystem")]
            filesystem: sandpolis_filesystem::FilesystemLayer::new()?,
            user: sandpolis_user::UserLayer::new(db.document("/user")?)?,
            agent: sandpolis_agent::AgentLayer::new(db.document("/agent")?).await?,
            server: sandpolis_server::ServerLayer::new(db.document("/server")?, network.clone())?,
            network,
            group,
            instance,
        })
    }
}

/// Layers are feature-sets that may be enabled on instances.
#[derive(
    Serialize, Deserialize, Clone, Copy, EnumIter, Debug, PartialEq, Eq, Hash, strum::Display,
)]
pub enum Layer {
    /// Manage accounts.
    #[cfg(feature = "layer-account")]
    Account,

    Agent,

    #[cfg(feature = "layer-audit")]
    Audit,

    Client,

    /// Deploy agents directly over a protocol like SSH or via special deployer
    /// packages.
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

    // Instance?
    /// View system information.
    #[cfg(feature = "layer-inventory")]
    Inventory,

    #[cfg(feature = "layer-location")]
    Location,

    /// Support for connecting to instances in the Sandpolis network and sending
    /// messages back and forth.
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

    #[cfg(feature = "layer-snapshot")]
    Snapshot,

    #[cfg(feature = "layer-sysinfo")]
    Sysinfo,

    /// Establish persistent or ephemeral tunnels between instances.
    #[cfg(feature = "layer-tunnel")]
    Tunnel,
}

/// All user accounts are subject to a set of permissions controlling what
/// server operations are authorized. The inital admin user has complete and
/// irrevocable permissions. By default, additional user accounts are created
/// without permissions and consequently are allowed to do almost nothing.
pub enum InstancePermission {
    #[cfg(feature = "layer-power")]
    Power(sandpolis_power::PowerPermission),
    #[cfg(feature = "layer-filesystem")]
    Filesystem(sandpolis_filesystem::FilesystemPermission),
}

macro_rules! layer_version {
    ($l:tt) => {
        LayerVersion {
            major: $l::built_info::PKG_VERSION_MAJOR.parse().unwrap(),
            minor: $l::built_info::PKG_VERSION_MINOR.parse().unwrap(),
            patch: $l::built_info::PKG_VERSION_PATCH.parse().unwrap(),
            description: if $l::built_info::PKG_DESCRIPTION != "" {
                Some($l::built_info::PKG_DESCRIPTION.to_string())
            } else {
                None
            },
        }
    };
}

// TODO make this const
pub fn layers() -> HashMap<Layer, LayerVersion> {
    HashMap::from([
        #[cfg(feature = "layer-shell")]
        (Layer::Shell, layer_version!(sandpolis_shell)),
    ])
}
