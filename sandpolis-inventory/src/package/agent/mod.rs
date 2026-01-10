use super::PackageData;
use anyhow::{Result, bail};
use std::path::PathBuf;

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
    async fn clean() -> Result<()> {
        bail!("Not implemented");
    }

    /// Get all currently installed packages.
    async fn get_installed() -> Result<Vec<PackageData>> {
        bail!("Not implemented");
    }

    /// Gather advanced metadata for the given package.
    async fn get_metadata(name: String) -> Result<PackageData> {
        bail!("Not implemented");
    }

    /// Get all packages that are currently outdated.
    async fn get_outdated() -> Result<Vec<PackageData>> {
        bail!("Not implemented");
    }

    /// Install the given packages onto the local system.
    async fn install(packages: Vec<String>) -> Result<()> {
        bail!("Not implemented");
    }

    /// Synchronize the local package database with all remote repositories.
    async fn refresh() -> Result<()> {
        bail!("Not implemented");
    }

    /// Remove the given packages from the local system.
    async fn remove(packages: Vec<String>) -> Result<()> {
        bail!("Not implemented");
    }

    /// Upgrade the given packages to the latest available version.
    async fn upgrade(packages: Vec<String>) -> Result<()> {
        bail!("Not implemented");
    }
}
