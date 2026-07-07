use super::PackageManager as PackageManagerTrait;
use super::apt::Apt;
use super::nix::Nix;
use super::pacman::Pacman;
use crate::package::PackageData;
use anyhow::Result;
use sandpolis_agent::Collector;
use sandpolis_instance::InstanceId;
use sandpolis_instance::database::{Data, RealmDatabase, ResidentVec};
use tracing::{debug, trace};

/// The concrete package manager detected on this host.
enum Manager {
    Apt(Apt),
    Nix(Nix),
    Pacman(Pacman),
}

impl Manager {
    /// Detect the first available package manager on the system.
    fn detect() -> Option<Self> {
        if Pacman::is_available() {
            Pacman::new().ok().map(Manager::Pacman)
        } else if Apt::is_available() {
            Apt::new().ok().map(Manager::Apt)
        } else if Nix::is_available() {
            Nix::new().ok().map(Manager::Nix)
        } else {
            None
        }
    }

    async fn get_installed(&self) -> Result<Vec<PackageData>> {
        match self {
            Manager::Apt(m) => m.get_installed().await,
            Manager::Nix(m) => m.get_installed().await,
            Manager::Pacman(m) => m.get_installed().await,
        }
    }
}

/// Collects installed packages from the host's package manager into the
/// database, scoped by instance. Mirrors the memory/user collectors.
pub struct PackageCollector {
    data: ResidentVec<PackageData>,
    manager: Option<Manager>,
    instance_id: InstanceId,
}

impl PackageCollector {
    pub fn new(db: RealmDatabase, instance_id: InstanceId) -> Result<Self> {
        Ok(Self {
            data: db.resident_vec(())?,
            manager: Manager::detect(),
            instance_id,
        })
    }
}

impl Collector for PackageCollector {
    async fn refresh(&mut self) -> Result<()> {
        let Some(manager) = self.manager.as_ref() else {
            trace!("No supported package manager detected");
            return Ok(());
        };

        let installed = manager.get_installed().await?;
        debug!(count = installed.len(), "Collected installed packages");

        // Update existing packages or add newly-installed ones.
        'next: for mut pkg in installed.iter().cloned() {
            pkg._instance_id = self.instance_id;
            for resident in self.data.iter() {
                if resident.read().name == pkg.name {
                    resident.update(|p| {
                        p.version = pkg.version.clone();
                        p.manager = pkg.manager.clone();
                        Ok(())
                    })?;
                    continue 'next;
                }
            }
            self.data.push(pkg)?;
        }

        // Remove packages that are no longer installed.
        let live: Vec<String> = installed.iter().map(|p| p.name.clone()).collect();
        let stale: Vec<_> = self
            .data
            .iter()
            .filter(|r| !live.contains(&r.read().name))
            .map(|r| r.read().id())
            .collect();
        for id in stale {
            self.data.remove(id)?;
        }

        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use sandpolis_instance::database::DatabaseLayer;
    use sandpolis_instance::realm::RealmName;
    use sandpolis_instance::test_db;

    #[tokio::test]
    #[test_log::test]
    #[ignore = "runs the host's real package manager"]
    async fn test_package_collector() -> Result<()> {
        let database: DatabaseLayer = test_db!(PackageData);

        let instance_id = InstanceId::new_server();
        let mut collector =
            PackageCollector::new(database.realm(RealmName::default())?, instance_id)?;
        collector.refresh().await?;

        for pkg in collector.data.iter() {
            assert_eq!(pkg.read()._instance_id, instance_id);
        }
        Ok(())
    }
}
