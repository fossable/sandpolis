use anyhow::{Result, bail};

pub trait PackageManager {

	/**
	 * Get the location of the package manager's binary on the filesystem.
	 *
	 * @return The package manager's file path
	 * @throws Exception
	 */
	public abstract Path getManagerLocation() throws Exception;

	/**
	 * Get the package manager's version string.
	 *
	 * @return The local version
	 * @throws Exception
	 */
	public abstract String getManagerVersion() throws Exception;

	/**
	 * Remove old packages from the local package cache.
	 *
	 * @throws Exception
	 */
	public abstract void clean() throws Exception;

	/**
	 * Get all currently installed packages.
	 *
	 * @return All locally installed packages
	 * @throws Exception
	 */
	public abstract List<Package> getInstalled() throws Exception;

	/**
	 * Gather advanced metadata for the given package.
	 *
	 * @param name The package name
	 * @return The package metadata
	 * @throws Exception
	 */
	public abstract Package getMetadata(String name) throws Exception;

	/**
	 * Get all packages that are currently outdated.
	 *
	 * @return All packages that have a newer version available
	 * @throws Exception
	 */
	public abstract List<Package> getOutdated() throws Exception;

	/// Install the given packages onto the local system.
	pub async fn install(packages: Vec<String>) -> Result<()> {
        todo!("Not implemented");
	}

	/// Synchronize the local package database with all remote repositories.
	pub async fn refresh() -> Result<()> {
        todo!("Not implemented");
	}

	/// Remove the given packages from the local system.
	pub async fn remove(packages: Vec<String>) -> Result<()> {
        todo!("Not implemented");
    }

    /// Upgrade the given packages to the latest available version.
    pub async fn upgrade(packages: Vec<String>) -> Result<()> {
        todo!("Not implemented");
    }
}
