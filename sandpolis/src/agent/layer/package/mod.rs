use anyhow::{bail, Result};

pub trait PackageManager {
    /// Get the location of the package manager's binary on the filesystem.
    pub fn getManagerLocation(&self) -> Result<PathBuf> {
        bail!("Not implemented");
    }

    /// Get the package manager's version string.
    pub async fn getManagerVersion(&self) -> Result<String> {
        bail!("Not implemented");
    }

    /// Remove old packages from the local package cache.
    pub async fn clean() -> Result<()> {
        bail!("Not implemented");
    }

    /// Get all currently installed packages.
    pub async fn getInstalled() -> Result<Vec<Package>> {
        bail!("Not implemented");
    }

    /// Gather advanced metadata for the given package.
    pub async fn getMetadata(name: String) -> Result<Package> {
        bail!("Not implemented");
    }

    /// Get all packages that are currently outdated.
    pub async fn getOutdated() -> Result<Vec<Package>> {
        bail!("Not implemented");
    }

    /// Install the given packages onto the local system.
    pub async fn install(packages: Vec<String>) -> Result<()> {
        bail!("Not implemented");
    }

    /// Synchronize the local package database with all remote repositories.
    pub async fn refresh() -> Result<()> {
        bail!("Not implemented");
    }

    /// Remove the given packages from the local system.
    pub async fn remove(packages: Vec<String>) -> Result<()> {
        bail!("Not implemented");
    }

    /// Upgrade the given packages to the latest available version.
    pub async fn upgrade(packages: Vec<String>) -> Result<()> {
        bail!("Not implemented");
    }
}
