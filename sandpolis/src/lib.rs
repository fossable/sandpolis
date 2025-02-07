use anyhow::bail;
use anyhow::Result;
use axum_macros::FromRef;
use sandpolis_database::Database;

pub mod cli;

/// Build info
pub mod built_info {
    include!(concat!(env!("OUT_DIR"), "/built.rs"));
}

#[derive(Clone, FromRef)]
pub struct InstanceState {
    pub db: Database,

    pub group: sandpolis_group::GroupLayer,
    #[cfg(feature = "layer-package")]
    pub package: sandpolis_package::PackageLayer,
    #[cfg(feature = "layer-shell")]
    pub power: sandpolis_power::PowerLayer,
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
            package: todo!(),
            power: todo!(),
            shell: todo!(),
            sysinfo: todo!(),
            user: todo!(),
            db,
        }
    }
}
