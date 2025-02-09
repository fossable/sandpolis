use anyhow::Result;
use axum_macros::FromRef;
use cli::CommandLine;
use sandpolis_database::Database;

pub mod cli;

/// Build info
pub mod built_info {
    include!(concat!(env!("OUT_DIR"), "/built.rs"));
}

#[derive(Clone, FromRef)]
pub struct InstanceState {
    pub agent: sandpolis_agent::AgentLayer,

    pub db: Database,
    #[cfg(feature = "layer-filesystem")]
    pub filesystem: sandpolis_filesystem::FilesystemLayer,
    #[cfg(feature = "server")]
    pub group: sandpolis_group::GroupLayer,
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
    pub async fn new(args: &CommandLine, db: Database) -> Result<Self> {
        let network =
            sandpolis_network::NetworkLayer::new(&args.network, db.document("/network")?)?;
        Ok(Self {
            #[cfg(feature = "server")]
            group: sandpolis_group::GroupLayer::new(db.document("/group")?)?,
            #[cfg(feature = "layer-package")]
            package: sandpolis_package::PackageLayer::new()?,
            #[cfg(feature = "layer-power")]
            power: sandpolis_power::PowerLayer::new()?,
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
            db,
        })
    }
}
