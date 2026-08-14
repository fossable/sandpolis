use anyhow::Result;
use native_db::Models;
use sandpolis_instance::LayerVersion;
use sandpolis_instance::database::DatabaseLayer;
use sandpolis_instance::realm::Realms;
use std::{collections::HashMap, sync::LazyLock};

#[cfg(feature = "agent")]
pub mod agent;
#[cfg(not(target_os = "android"))]
pub mod cli;
#[cfg(feature = "client")]
pub mod client;
pub mod config;
#[cfg(feature = "client")]
pub mod lsp;
#[cfg(feature = "server")]
pub mod server;

/// Re-exported so embedders (the mobile app) can name the stratum without
/// depending on `sandpolis-server` directly.
pub use sandpolis_server::ServerStratum;

/// Everything this process was told to do on the command line, plus what the
/// `.server` file it was given contributes.
///
/// This is plumbing, not configuration: nothing here is serialized or read from
/// disk. Realm-scoped settings live in `.realm` files ([`config::RealmConfig`]);
/// what's here describes *this process*.
#[cfg_attr(feature = "client", derive(bevy::prelude::Resource))]
#[derive(Clone)]
pub struct RuntimeOptions {
    pub database: sandpolis_instance::database::config::DatabaseConfig,

    /// What this process was started as, which is what decides the type of its
    /// [`sandpolis_instance::InstanceId`]. One process is exactly one instance.
    pub instance_type: sandpolis_instance::InstanceType,

    /// Where this process's server binds.
    #[cfg(feature = "server")]
    pub listen: std::net::SocketAddr,

    /// Addresses rejected before authentication runs.
    #[cfg(feature = "server")]
    pub blocked_ips: Vec<std::net::IpAddr>,

    /// Frame rate for the GUI and TUI.
    #[cfg(feature = "client")]
    pub fps: u32,

    /// Polling connection mode, from the `.server` file.
    #[cfg(feature = "agent")]
    pub poll: Option<sandpolis_instance::realm::config::PollConfig>,

    /// The server this agent attaches to, named by its `.server` file.
    #[cfg(feature = "agent")]
    pub server: Option<sandpolis_server::ServerUrl>,

    /// Realms this server was told to serve, one per `--realm` file.
    #[cfg(feature = "server")]
    pub realms: Vec<crate::config::RealmConfig>,
}

impl Default for RuntimeOptions {
    fn default() -> Self {
        Self {
            database: Default::default(),
            instance_type: sandpolis_instance::InstanceType::Client,
            #[cfg(feature = "server")]
            listen: std::net::SocketAddr::new(
                std::net::IpAddr::V4(std::net::Ipv4Addr::UNSPECIFIED),
                sandpolis_server::ServerUrl::default_port(),
            ),
            #[cfg(feature = "server")]
            blocked_ips: Vec::new(),
            #[cfg(feature = "client")]
            fps: 30,
            #[cfg(feature = "agent")]
            poll: None,
            #[cfg(feature = "agent")]
            server: None,
            #[cfg(feature = "server")]
            realms: Vec::new(),
        }
    }
}

impl RuntimeOptions {
    /// Defaults for a client that only connects out — the mobile app, an
    /// example — where no command line was parsed.
    pub fn embedded() -> Self {
        Self::default()
    }

    /// The account sections of every loaded realm, merged.
    ///
    /// Accounts are still seeded into the default realm, so a multi-realm server
    /// pools them for now. Per-realm account layering is a larger change than
    /// this file format.
    // TODO seed each realm's accounts into that realm's database
    #[cfg(all(feature = "server", feature = "account"))]
    pub fn merged_account_config(&self) -> sandpolis_account::config::AccountLayerConfig {
        let mut merged = sandpolis_account::config::AccountLayerConfig::default();
        for (index, realm) in self.realms.iter().enumerate() {
            if index == 0 {
                merged.scrape = realm.account.scrape.clone();
            }
            merged
                .accounts
                .extend(realm.account.accounts.iter().cloned());
        }
        merged
    }

    /// The probe sections of every loaded realm, merged. See
    /// [`merged_account_config`](Self::merged_account_config) for why.
    #[cfg(all(feature = "server", feature = "probe"))]
    pub fn merged_probe_config(&self) -> sandpolis_probe::config::ProbeLayerConfig {
        sandpolis_probe::config::ProbeLayerConfig {
            devices: self
                .realms
                .iter()
                .flat_map(|realm| realm.probe.devices.iter().cloned())
                .collect(),
        }
    }
}

/// Load the `.server` file given on the command line, if any.
///
/// Returns the certificate it holds — which names exactly one server and realm —
/// along with any polling settings written alongside it.
#[cfg(not(target_os = "android"))]
pub fn load_server_file(
    path: Option<&std::path::Path>,
) -> Result<
    Option<(
        sandpolis_instance::realm::config::EndpointCert,
        Option<sandpolis_instance::realm::config::PollConfig>,
    )>,
> {
    let Some(path) = path else {
        return Ok(None);
    };
    Ok(Some(
        sandpolis_instance::realm::config::ServerCertFile::load(path)?,
    ))
}

/// Which stratum a server runs in.
///
/// A `.server` file means this server attaches to the one it names, which puts
/// it in the local stratum. Without one, this is the network's single global
/// stratum server.
#[cfg(not(target_os = "android"))]
pub fn stratum(server_file: Option<&std::path::Path>) -> Result<ServerStratum> {
    Ok(match load_server_file(server_file)? {
        Some((cert, _)) => ServerStratum::Local {
            global: cert.url()?,
        },
        None => ServerStratum::Global,
    })
}

#[cfg_attr(feature = "client", derive(bevy::prelude::Resource))]
#[cfg_attr(feature = "server", derive(axum_macros::FromRef))]
#[derive(Clone)]
pub struct InstanceState {
    #[cfg(feature = "account")]
    pub account: sandpolis_account::AccountLayer,
    pub agent: sandpolis_agent::AgentLayer,
    #[cfg(feature = "desktop")]
    pub desktop: sandpolis_desktop::DesktopLayer,
    #[cfg(feature = "filesystem")]
    pub filesystem: sandpolis_filesystem::FilesystemLayer,
    #[cfg(feature = "health")]
    pub health: sandpolis_health::HealthLayer,
    pub realms: Realms,
    pub instance: sandpolis_instance::InstanceLayer,
    pub network: sandpolis_instance::network::NetworkLayer,
    #[cfg(feature = "inventory")]
    pub inventory: sandpolis_inventory::InventoryLayer,
    pub server: sandpolis_server::ServerLayer,
    #[cfg(feature = "shell")]
    pub shell: sandpolis_shell::ShellLayer,
    #[cfg(feature = "snapshot")]
    pub snapshot: sandpolis_snapshot::SnapshotLayer,
    #[cfg(feature = "probe")]
    pub probe: sandpolis_probe::ProbeLayer,
    pub user: sandpolis_server::user::UserLayer,
}

impl InstanceState {
    /// `stratum` describes the server this process runs (if any). On builds
    /// without the server feature it is inert, since nothing consults it.
    ///
    /// `realms` is built by the caller, which is the only place that knows the
    /// `.realm` and `.server` files this process was given.
    pub async fn new(
        options: &RuntimeOptions,
        database: DatabaseLayer,
        realms: Realms,
        stratum: sandpolis_server::ServerStratum,
    ) -> Result<Self> {
        // Create all the configured layers, starting with the most foundational

        let instance =
            sandpolis_instance::InstanceLayer::new(database.clone(), options.instance_type).await?;

        let network = sandpolis_instance::network::NetworkLayer::new(database.clone()).await?;

        let server = sandpolis_server::ServerLayer::new(
            database.clone(),
            network.clone(),
            realms.clone(),
            stratum,
            options.instance_type,
        )
        .await?;

        let user = sandpolis_server::user::UserLayer::new(
            instance.clone(),
            database.clone(),
            network.clone(),
            #[cfg(feature = "server")]
            server.stratum.clone(),
            #[cfg(feature = "server")]
            server.ownership.clone(),
        )
        .await?;

        let agent = sandpolis_agent::AgentLayer::new(database.clone()).await?;

        // Deployment mints an agent certificate for whichever realm the new
        // agent will connect to. Its responder is built by the stateless
        // `inventory` factory and holds no state of its own, so this is how the
        // realm handles reach it.
        #[cfg(feature = "server")]
        sandpolis_agent::deploy::server::install_realms(realms.clone());

        #[cfg(feature = "inventory")]
        let inventory =
            sandpolis_inventory::InventoryLayer::new(database.clone(), instance.clone()).await?;

        #[cfg(feature = "health")]
        let health = sandpolis_health::HealthLayer::new(database.clone(), instance.clone()).await?;

        #[cfg(feature = "shell")]
        let shell = sandpolis_shell::ShellLayer::new(database.clone()).await?;

        #[cfg(feature = "filesystem")]
        let filesystem = sandpolis_filesystem::FilesystemLayer::new().await?;

        #[cfg(feature = "desktop")]
        let desktop = sandpolis_desktop::DesktopLayer::new(database.clone()).await?;

        #[cfg(feature = "account")]
        let account = sandpolis_account::AccountLayer::new(database.clone()).await?;

        #[cfg(feature = "snapshot")]
        let snapshot = sandpolis_snapshot::SnapshotLayer::new().await?;

        #[cfg(feature = "probe")]
        let probe = sandpolis_probe::ProbeLayer::new(
            {
                #[cfg(feature = "server")]
                {
                    options.merged_probe_config()
                }
                #[cfg(not(feature = "server"))]
                {
                    Default::default()
                }
            },
            instance.instance_id,
        );

        Ok(Self {
            #[cfg(feature = "inventory")]
            inventory,
            #[cfg(feature = "health")]
            health,
            #[cfg(feature = "shell")]
            shell,
            #[cfg(feature = "filesystem")]
            filesystem,
            #[cfg(feature = "desktop")]
            desktop,
            #[cfg(feature = "snapshot")]
            snapshot,
            #[cfg(feature = "probe")]
            probe,
            #[cfg(feature = "account")]
            account,
            user,
            agent,
            server,
            network,
            realms,
            instance,
        })
    }
}

/// All user accounts are subject to a set of permissions controlling what
/// server operations are authorized. The inital admin user has complete and
/// irrevocable permissions. By default, additional user accounts are created
/// without permissions and consequently are allowed to do almost nothing.
pub enum InstancePermission {
    Wake(sandpolis_agent::wake::WakePermission),
    #[cfg(feature = "filesystem")]
    Filesystem(sandpolis_filesystem::FilesystemPermission),
}

// TODO inventory crate
pub fn layers() -> HashMap<sandpolis_instance::LayerName, LayerVersion> {
    HashMap::from([])
}

// TODO dynamic loading
pub static MODELS: LazyLock<Models> = LazyLock::new(|| {
    let mut m = Models::new();

    // Network layer
    {
        m.define::<sandpolis_instance::network::NetworkLayerData>()
            .unwrap();
        m.define::<sandpolis_instance::network::ConnectionData>()
            .unwrap();
        // m.define::<sandpolis_instance::network::ServerConnectionData>()
        //     .unwrap();
    }

    // Realm layer
    {
        m.define::<sandpolis_instance::realm::RealmData>().unwrap();
        m.define::<sandpolis_instance::realm::RealmClusterCert>()
            .unwrap();
        m.define::<sandpolis_instance::realm::RealmServerCert>()
            .unwrap();
        m.define::<sandpolis_instance::realm::RealmClientCert>()
            .unwrap();
        m.define::<sandpolis_instance::realm::RealmAgentCert>()
            .unwrap();
    }

    // Instance layer
    {
        m.define::<sandpolis_instance::InstanceLayerData>().unwrap();
        m.define::<sandpolis_instance::domain::DomainData>()
            .unwrap();
        m.define::<sandpolis_instance::service::ServiceData>()
            .unwrap();
        m.define::<sandpolis_instance::notification::NotificationData>()
            .unwrap();
        // How far this client has surfaced notifications. Client-local, so it
        // is neither defined nor replicated anywhere else.
        #[cfg(feature = "client")]
        m.define::<sandpolis_client::notification::NotificationWatermarkData>()
            .unwrap();
    }

    // User layer
    {
        m.define::<sandpolis_server::user::UserLayerData>().unwrap();
        m.define::<sandpolis_server::user::UserData>().unwrap();
        #[cfg(feature = "server")]
        m.define::<sandpolis_server::user::server::PasswordData>()
            .unwrap();
        #[cfg(feature = "server")]
        m.define::<sandpolis_server::user::server::ServerJwtSecret>()
            .unwrap();
    }

    // Server layer
    {
        m.define::<sandpolis_server::ServerLayerData>().unwrap();
        #[cfg(feature = "server")]
        m.define::<sandpolis_server::banner::ServerBannerData>()
            .unwrap();
        #[cfg(feature = "server")]
        m.define::<sandpolis_server::ownership::OwnershipData>()
            .unwrap();
        #[cfg(feature = "client")]
        m.define::<sandpolis_server::client::SavedServerData>()
            .unwrap();
    }

    // Shell layer
    #[cfg(feature = "shell")]
    {
        m.define::<sandpolis_shell::ShellSessionData>().unwrap();
    }

    // Desktop layer
    #[cfg(feature = "desktop")]
    {
        m.define::<sandpolis_desktop::DesktopData>().unwrap();
    }

    // Account layer
    #[cfg(feature = "account")]
    {
        m.define::<sandpolis_account::AccountLayerData>().unwrap();
        m.define::<sandpolis_account::AccountData>().unwrap();
        m.define::<sandpolis_account::AccountLinkData>().unwrap();
        m.define::<sandpolis_account::favicon::FaviconData>()
            .unwrap();
    }

    // Health layer
    #[cfg(feature = "health")]
    {
        m.define::<sandpolis_health::HealthLayerData>().unwrap();
        m.define::<sandpolis_health::systemd::SystemdUnitData>()
            .unwrap();
    }

    // Inventory layer
    #[cfg(feature = "inventory")]
    {
        m.define::<sandpolis_inventory::InventoryLayerData>()
            .unwrap();
        m.define::<sandpolis_inventory::hardware::display::DisplayData>()
            .unwrap();
        m.define::<sandpolis_inventory::hardware::firmware::FirmwareData>()
            .unwrap();
        m.define::<sandpolis_inventory::hardware::memory::MemoryDeviceData>()
            .unwrap();
        m.define::<sandpolis_inventory::hardware::battery::BatteryData>()
            .unwrap();
        m.define::<sandpolis_inventory::hardware::cpu::CpuData>()
            .unwrap();
        m.define::<sandpolis_inventory::hardware::cpu::CpuCoreData>()
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
