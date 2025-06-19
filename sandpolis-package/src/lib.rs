use anyhow::Result;
use native_db::ToKey;
use native_model::Model;
use sandpolis_database::{Data, DataIdentifier};
use sandpolis_macros::data;
use serde::{Deserialize, Serialize};

// message RQ_InstallOrUpgradePackages {
//     repeated string package = 1;
// }

// message RQ_RemovePackages {
//     repeated string package = 1;
// }

// message RQ_RefreshPackages {
// }

#[cfg(feature = "agent")]
pub mod agent;

#[derive(Clone)]
pub struct PackageLayer {}

impl PackageLayer {
    pub async fn new() -> Result<Self> {
        Ok(Self {})
    }
}

pub fn pacman() {}

// pub fn packages_iter() -> impl Iterator<Item = Package> {}

#[derive(Serialize, Deserialize, Debug, Clone, PartialEq, Default)]
pub enum PackageManager {
    Pacman,
    Apt,
    Nix,
    #[default]
    Unknown,
}

#[data]
pub struct PackageManagerData {
    /// Type of package manager
    pub manager: PackageManager,

    /// Number of installed packages
    pub installed_packages: u64,

    /// Number of cached packages
    pub cached_packages: Option<u64>,
}

#[data]
pub struct PackageData {
    /// Canonical name/identifier
    pub name: String,

    /// Package version string
    pub version: String,

    /// Type of package manager that manages this package
    pub manager: PackageManager,

    /// Textual description of the package
    pub description: Option<String>,

    /// Latest available version of the package that can be installed
    pub latest_available: Option<String>,

    /// Latest version of the upstream package
    pub latest_upstream: Option<String>,

    /// System architecture type
    pub architecture: Option<String>,

    /// Package size in bytes as an archive
    pub package_size: Option<u64>,

    /// Package size when extracted and installed
    pub installed_size: Option<u64>,

    /// Package homepage URL
    pub upstream_url: Option<String>,

    /// URL to download the package
    pub remote_location: Option<String>,

    /// Remote repository URL
    pub repository: Option<String>,

    /// File contents of the package
    pub files: Option<Vec<String>>,

    /// Whether the package was explicitly installed or installed as a
    /// dependency of another package
    pub explicit: Option<bool>,

    /// Licenses under which the package is distributed
    pub licenses: Option<Vec<String>>,

    /// Other packages that this package requires
    pub dependencies: Option<Vec<String>>,

    /// Other packages that require this package
    pub usages: Option<Vec<String>>,

    /// Epoch timestamp when the package was built
    pub build_time: Option<u64>,

    /// Epoch timestamp when the package was most recently installed
    pub install_time: Option<u64>,
}
