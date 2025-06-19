use native_db::ToKey;
use native_model::Model;
use sandpolis_core::InstanceId;
use sandpolis_database::DbTimestamp;
use sandpolis_macros::data;
use serde::{Deserialize, Serialize};

pub mod group;
pub mod memory;
pub mod mountpoint;
pub mod network;
pub mod process;
pub mod user;

#[data(history)]
pub struct OsData {
    #[secondary_key]
    pub _instance_id: InstanceId,

    /// Distribution name
    pub name: String,
    /// The operating system's family
    pub family: Option<os_info::Type>,
    /// The operating system's manufacturer
    pub manufacturer: String,
    /// The operating system's register width in bits
    pub bitness: Option<os_info::Bitness>,
    /// The operating system's primary version
    pub version: String,
    /// Major release version
    pub major_version: Option<String>,
    /// Minor release version
    pub minor_version: Option<String>,
    /// The operating system's code name
    pub codename: Option<String>,
    /// The operating system's build number
    pub build_number: Option<String>,
    /// OS architecture
    pub arch: String,
}

#[data(history)]
pub struct KernelModuleData {
    #[secondary_key]
    pub _instance_id: InstanceId,

    /// Module name
    #[secondary_key]
    pub name: String,

    /// Size of module content
    pub size: String,
    /// Module reverse dependencies
    pub used_by: String,
    /// Kernel module status
    pub status: String,
    /// Kernel module address
    pub address: String,
}
