use anyhow::Result;
use axum_macros::FromRef;
use config::Configuration;
use native_db::Models;
use sandpolis_database::DatabaseLayer;
use sandpolis_instance::LayerVersion;
use serde::{Deserialize, Serialize};
use std::{collections::HashMap, sync::LazyLock};
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

#[cfg_attr(feature = "client-gui", derive(bevy::prelude::Resource))]
#[derive(Clone, FromRef)]
pub struct InstanceState {
    #[cfg(feature = "layer-account")]
    pub account: sandpolis_account::AccountLayer,
    pub agent: sandpolis_agent::AgentLayer,
    #[cfg(feature = "layer-desktop")]
    pub desktop: sandpolis_desktop::DesktopLayer,
    #[cfg(feature = "layer-filesystem")]
    pub filesystem: sandpolis_filesystem::FilesystemLayer,
    pub realm: sandpolis_realm::RealmLayer,
    pub instance: sandpolis_instance::InstanceLayer,
    pub network: sandpolis_network::NetworkLayer,
    #[cfg(feature = "layer-inventory")]
    pub inventory: sandpolis_inventory::InventoryLayer,
    #[cfg(feature = "layer-power")]
    pub power: sandpolis_power::PowerLayer,
    pub server: sandpolis_server::ServerLayer,
    #[cfg(feature = "layer-shell")]
    pub shell: sandpolis_shell::ShellLayer,
    #[cfg(feature = "layer-snapshot")]
    pub snapshot: sandpolis_snapshot::SnapshotLayer,
    pub user: sandpolis_user::UserLayer,
}

impl InstanceState {
    pub async fn new(config: Configuration, database: DatabaseLayer) -> Result<Self> {
        // Create all the configured layers, starting with the most foundational

        let instance = sandpolis_instance::InstanceLayer::new(database.clone()).await?;

        let realm =
            sandpolis_realm::RealmLayer::new(config.realm, database.clone(), instance.clone())
                .await?;

        let network =
            sandpolis_network::NetworkLayer::new(config.network, database.clone(), realm.clone())
                .await?;

        let user = sandpolis_user::UserLayer::new(instance.clone(), database.clone()).await?;

        let agent = sandpolis_agent::AgentLayer::new(database.clone()).await?;

        let server = sandpolis_server::ServerLayer::new(database.clone(), network.clone()).await?;

        #[cfg(feature = "layer-inventory")]
        let inventory =
            sandpolis_inventory::InventoryLayer::new(database.clone(), instance.clone()).await?;

        #[cfg(feature = "layer-power")]
        let power = sandpolis_power::PowerLayer {
            network: network.clone(),
        };

        #[cfg(feature = "layer-shell")]
        let shell = sandpolis_shell::ShellLayer::new(database.clone()).await?;

        #[cfg(feature = "layer-filesystem")]
        let filesystem = sandpolis_filesystem::FilesystemLayer::new().await?;

        #[cfg(feature = "layer-desktop")]
        let desktop = sandpolis_desktop::DesktopLayer::new().await?;

        #[cfg(feature = "layer-account")]
        let account = sandpolis_account::AccountLayer::new(database.clone()).await?;

        #[cfg(feature = "layer-snapshot")]
        let snapshot = sandpolis_snapshot::SnapshotLayer::new().await?;

        Ok(Self {
            #[cfg(feature = "layer-inventory")]
            inventory,
            #[cfg(feature = "layer-power")]
            power,
            #[cfg(feature = "layer-shell")]
            shell,
            #[cfg(feature = "layer-filesystem")]
            filesystem,
            #[cfg(feature = "layer-desktop")]
            desktop,
            #[cfg(feature = "layer-snapshot")]
            snapshot,
            #[cfg(feature = "layer-account")]
            account,
            user,
            agent,
            server,
            network,
            realm,
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

    /// Support for connecting to instances in the Sandpolis network and sending
    /// messages back and forth.
    Network,
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

pub static MODELS: LazyLock<Models> = LazyLock::new(|| {
    let mut m = Models::new();

    // Network layer
    {
        m.define::<sandpolis_network::NetworkLayerData>().unwrap();
        m.define::<sandpolis_network::ConnectionData>().unwrap();
        // m.define::<sandpolis_network::ServerConnectionData>()
        //     .unwrap();
    }

    // Realm layer
    {
        m.define::<sandpolis_realm::RealmLayerData>().unwrap();
        m.define::<sandpolis_realm::RealmData>().unwrap();
        m.define::<sandpolis_realm::RealmClusterCert>().unwrap();
        m.define::<sandpolis_realm::RealmServerCert>().unwrap();
        m.define::<sandpolis_realm::RealmClientCert>().unwrap();
        m.define::<sandpolis_realm::RealmAgentCert>().unwrap();
    }

    // Instance layer
    {
        m.define::<sandpolis_instance::InstanceLayerData>().unwrap();
    }

    // User layer
    {
        m.define::<sandpolis_user::UserLayerData>().unwrap();
        m.define::<sandpolis_user::UserData>().unwrap();
        #[cfg(feature = "server")]
        m.define::<sandpolis_user::server::PasswordData>().unwrap();
        #[cfg(feature = "server")]
        m.define::<sandpolis_user::server::ServerJwtSecret>()
            .unwrap();
    }

    // Server layer
    {
        m.define::<sandpolis_server::ServerLayerData>().unwrap();
        #[cfg(feature = "server")]
        m.define::<sandpolis_server::server::ServerBannerData>()
            .unwrap();
        #[cfg(feature = "client")]
        m.define::<sandpolis_server::client::SavedServerData>()
            .unwrap();
    }

    // Shell layer
    #[cfg(feature = "layer-shell")]
    {
        m.define::<sandpolis_shell::ShellSessionData>().unwrap();
    }

    // Account layer
    #[cfg(feature = "layer-account")]
    {
        m.define::<sandpolis_account::AccountLayerData>().unwrap();
    }

    // Inventory layer
    #[cfg(feature = "layer-inventory")]
    {
        m.define::<sandpolis_inventory::InventoryLayerData>()
            .unwrap();
        m.define::<sandpolis_inventory::hardware::display::DisplayData>()
            .unwrap();
        m.define::<sandpolis_inventory::hardware::firmware::FirmwareData>()
            .unwrap();
        m.define::<sandpolis_inventory::hardware::memory::MemoryData>()
            .unwrap();
        m.define::<sandpolis_inventory::hardware::battery::BatteryData>()
            .unwrap();
        m.define::<sandpolis_inventory::os::OsData>().unwrap();
        m.define::<sandpolis_inventory::os::user::UserData>()
            .unwrap();
        m.define::<sandpolis_inventory::os::group::GroupData>()
            .unwrap();
        m.define::<sandpolis_inventory::os::mountpoint::MountpointData>()
            .unwrap();
        m.define::<sandpolis_inventory::os::process::ProcessData>()
            .unwrap();
        m.define::<sandpolis_inventory::os::memory::MemoryData>()
            .unwrap();
        m.define::<sandpolis_inventory::os::network::NetworkData>()
            .unwrap();
        m.define::<sandpolis_inventory::os::KernelModuleData>()
            .unwrap();
        m.define::<sandpolis_inventory::package::PackageManagerData>()
            .unwrap();
        m.define::<sandpolis_inventory::package::PackageData>()
            .unwrap();
    }

    m
});
