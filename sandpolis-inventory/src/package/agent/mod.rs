use super::PackageData;
use anyhow::{Result, bail};
use std::path::PathBuf;

pub mod apt;
pub mod collector;
pub mod nix;
pub mod pacman;

pub use collector::PackageCollector;

/// A local package manager the agent can inspect and drive. Every method takes
/// `&self` so it operates through the located executable rather than assuming
/// the binary is on `PATH`. Management operations (install/remove/upgrade/…) are
/// implemented ahead of the UI that will drive them.
#[allow(dead_code)]
pub(crate) trait PackageManager {
    /// Get the location of the package manager's binary on the filesystem.
    fn get_location(&self) -> Result<PathBuf> {
        bail!("Not implemented");
    }

    /// Get the package manager's version string.
    async fn get_version(&self) -> Result<String> {
        bail!("Not implemented");
    }

    /// Remove old packages from the local package cache.
    async fn clean(&self) -> Result<()> {
        bail!("Not implemented");
    }

    /// Get all currently installed packages.
    async fn get_installed(&self) -> Result<Vec<PackageData>> {
        bail!("Not implemented");
    }

    /// Gather advanced metadata for the given package.
    async fn get_metadata(&self, _name: String) -> Result<PackageData> {
        bail!("Not implemented");
    }

    /// Get all packages that are currently outdated.
    async fn get_outdated(&self) -> Result<Vec<PackageData>> {
        bail!("Not implemented");
    }

    /// Install the given packages onto the local system.
    async fn install(&self, _packages: Vec<String>) -> Result<()> {
        bail!("Not implemented");
    }

    /// Synchronize the local package database with all remote repositories.
    async fn refresh(&self) -> Result<()> {
        bail!("Not implemented");
    }

    /// Remove the given packages from the local system.
    async fn remove(&self, _packages: Vec<String>) -> Result<()> {
        bail!("Not implemented");
    }

    /// Upgrade the given packages to the latest available version.
    async fn upgrade(&self, _packages: Vec<String>) -> Result<()> {
        bail!("Not implemented");
    }
}
