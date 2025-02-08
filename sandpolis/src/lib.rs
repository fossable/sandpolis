use axum_macros::FromRef;
use sandpolis_database::Database;

pub mod cli;

/// Build info
pub mod built_info {
    include!(concat!(env!("OUT_DIR"), "/built.rs"));
}

#[derive(Clone, FromRef)]
pub struct InstanceState {
    #[cfg(feature = "agent")]
    pub agent: sandpolis_agent::agent::AgentLayer,

    pub db: Database,
    #[cfg(feature = "layer-filesystem")]
    pub filesystem: sandpolis_filesystem::FilesystemLayer,
    pub group: sandpolis_group::GroupLayer,
    pub network: sandpolis_network::NetworkLayer,
    #[cfg(feature = "layer-package")]
    pub package: sandpolis_package::PackageLayer,
    #[cfg(feature = "layer-shell")]
    pub power: sandpolis_power::PowerLayer,
    #[cfg(feature = "server")]
    pub server: sandpolis_server::ServerLayer,
    #[cfg(feature = "layer-shell")]
    pub shell: sandpolis_shell::ShellLayer,
    #[cfg(feature = "layer-sysinfo")]
    pub sysinfo: sandpolis_sysinfo::SysinfoLayer,
    pub user: sandpolis_user::UserLayer,
}

impl InstanceState {
    pub fn new(db: Database) -> Self {
        Self {
            group: todo!(),
            #[cfg(feature = "layer-package")]
            package: todo!(),
            power: todo!(),
            #[cfg(feature = "layer-shell")]
            shell: todo!(),
            #[cfg(feature = "layer-sysinfo")]
            sysinfo: todo!(),
            #[cfg(feature = "layer-filesystem")]
            filesystem: todo!(),
            user: todo!(),
            network: todo!(),
            #[cfg(feature = "agent")]
            agent: todo!(),
            #[cfg(feature = "server")]
            server: todo!(),
            db,
        }
    }
}
