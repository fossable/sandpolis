use anyhow::Result;
use axum_macros::FromRef;
use config::Configuration;
use sandpolis_database::Database;
use serde::{Deserialize, Serialize};
use strum::EnumIter;

pub mod cli;
pub mod config;

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
#[derive(Serialize, Deserialize, Clone, Copy, EnumIter, Debug, PartialEq, Eq, Hash)]
pub enum Layer {
    /// Manage accounts.
    #[cfg(feature = "layer-account")]
    Account,

    Agent,

    #[cfg(feature = "layer-alert")]
    Alert,

    Client,

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
