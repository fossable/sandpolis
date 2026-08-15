use anyhow::Result;
use sandpolis_instance::LayerVersion;
use sandpolis_instance::database::DatabaseManager;
use sandpolis_instance::realm::RealmManager;
use std::collections::HashMap;

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
    pub fn merged_account_config(&self) -> sandpolis_account::config::AccountManagerConfig {
        let mut merged = sandpolis_account::config::AccountManagerConfig::default();
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
    pub fn merged_probe_config(&self) -> sandpolis_probe::config::ProbeManagerConfig {
        sandpolis_probe::config::ProbeManagerConfig {
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
        sandpolis_instance::realm::RealmCert,
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

/// Bring up the state an endpoint instance — an agent or a client — runs on.
///
/// The two differ only in how they came by their certificate. Past that they
/// agree: each owns its database outright (the server it attaches to replicates
/// *from* it), each knows exactly the realms its certificate names, and neither
/// runs a server of its own, so the stratum it reports is inert.
#[cfg(not(target_os = "android"))]
pub async fn endpoint_state(
    options: &RuntimeOptions,
    endpoint_certs: Vec<sandpolis_instance::realm::RealmCert>,
) -> Result<InstanceState> {
    use sandpolis_instance::database::WriteAuthority;

    let database = DatabaseManager::new(options.database.clone(), &MODELS, WriteAuthority::Full)?;
    let realms = RealmManager::for_endpoint(endpoint_certs, database.clone())?;

    InstanceState::new(options, database, realms, ServerStratum::Global).await
}

/// Which stratum a server runs in.
///
/// A `.server` file means this server attaches to the one it names, which puts
/// it in the local stratum. Without one, this is the network's single global
/// stratum server.
#[cfg(not(target_os = "android"))]
pub fn stratum(server_file: Option<&std::path::Path>) -> Result<ServerStratum> {
    stratum_of(load_server_file(server_file)?.as_ref())
}

/// [`stratum`], for a caller that already loaded the `.server` file. The server
/// itself needs the certificate for more than this, so it reads the file once
/// and asks here rather than going back to disk.
#[cfg(not(target_os = "android"))]
pub fn stratum_of(
    endpoint: Option<
        &(
            sandpolis_instance::realm::RealmCert,
            Option<sandpolis_instance::realm::config::PollConfig>,
        ),
    >,
) -> Result<ServerStratum> {
    Ok(match endpoint {
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
    pub account: sandpolis_account::AccountManager,
    pub agent: sandpolis_agent::AgentManager,
    #[cfg(feature = "desktop")]
    pub desktop: sandpolis_desktop::DesktopManager,
    #[cfg(feature = "filesystem")]
    pub filesystem: sandpolis_filesystem::FilesystemManager,
    #[cfg(feature = "health")]
    pub health: sandpolis_health::HealthManager,
    pub realms: RealmManager,
    pub instance: sandpolis_instance::InstanceManager,
    pub network: sandpolis_instance::network::NetworkManager,
    #[cfg(feature = "inventory")]
    pub inventory: sandpolis_inventory::InventoryManager,
    pub server: sandpolis_server::ServerManager,
    #[cfg(feature = "shell")]
    pub shell: sandpolis_shell::ShellManager,
    #[cfg(feature = "snapshot")]
    pub snapshot: sandpolis_snapshot::SnapshotManager,
    #[cfg(feature = "probe")]
    pub probe: sandpolis_probe::ProbeManager,
    pub user: sandpolis_server::user::UserManager,
}

impl InstanceState {
    /// `stratum` describes the server this process runs (if any). On builds
    /// without the server feature it is inert, since nothing consults it.
    ///
    /// `realms` is built by the caller, which is the only place that knows the
    /// `.realm` and `.server` files this process was given.
    pub async fn new(
        options: &RuntimeOptions,
        database: DatabaseManager,
        realms: RealmManager,
        stratum: sandpolis_server::ServerStratum,
    ) -> Result<Self> {
        // Create all the configured subsystems, starting with the most foundational

        let instance =
            sandpolis_instance::InstanceManager::new(database.clone(), options.instance_type).await?;

        let network = sandpolis_instance::network::NetworkManager::new(database.clone()).await?;

        let server = sandpolis_server::ServerManager::new(
            database.clone(),
            network.clone(),
            realms.clone(),
            stratum,
            options.instance_type,
        )
        .await?;

        let user = sandpolis_server::user::UserManager::new(
            instance.clone(),
            database.clone(),
            network.clone(),
            #[cfg(feature = "server")]
            server.stratum.clone(),
            #[cfg(feature = "server")]
            server.ownership.clone(),
        )
        .await?;

        let agent = sandpolis_agent::AgentManager::new(database.clone()).await?;

        // Deployment mints an agent certificate for whichever realm the new
        // agent will connect to. Its responder is built by the stateless
        // `inventory` factory and holds no state of its own, so this is how the
        // realm handles reach it.
        #[cfg(feature = "server")]
        sandpolis_agent::deploy::server::install_realms(realms.clone());

        #[cfg(feature = "inventory")]
        let inventory =
            sandpolis_inventory::InventoryManager::new(database.clone(), instance.clone()).await?;

        #[cfg(feature = "health")]
        let health = sandpolis_health::HealthManager::new(database.clone(), instance.clone()).await?;

        #[cfg(feature = "shell")]
        let shell = sandpolis_shell::ShellManager::new(database.clone()).await?;

        #[cfg(feature = "filesystem")]
        let filesystem = sandpolis_filesystem::FilesystemManager::new().await?;

        #[cfg(feature = "desktop")]
        let desktop = sandpolis_desktop::DesktopManager::new(database.clone()).await?;

        #[cfg(feature = "account")]
        let account = sandpolis_account::AccountManager::new(database.clone()).await?;

        #[cfg(feature = "snapshot")]
        let snapshot = sandpolis_snapshot::SnapshotManager::new().await?;

        #[cfg(feature = "probe")]
        let probe = sandpolis_probe::ProbeManager::new(
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

/// Every model linked into this build, collected from the `#[data]` macro's
/// registrations. Re-exported here because this is where callers expect it.
pub use sandpolis_instance::database::MODELS;

#[cfg(test)]
mod test_models {
    /// Building the registry is what proves the estate's models agree: every
    /// `#[data]` type registers itself, and `define` rejects a duplicate
    /// native_model id, so a collision between two subsystems fails here rather
    /// than when an instance starts.
    ///
    /// This binary links every subsystem, which is what makes the check meaningful.
    #[test]
    fn models_have_no_id_collisions() {
        let _ = &*super::MODELS;
    }
}
