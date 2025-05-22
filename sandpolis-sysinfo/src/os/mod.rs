use native_db::*;
use native_model::{Model, native_model};
use sandpolis_database::DbTimestamp;
use sandpolis_instance::InstanceId;
use serde::{Deserialize, Serialize};

pub mod memory;
pub mod mountpoint;
pub mod network;
pub mod process;
pub mod user;

#[derive(Serialize, Deserialize, PartialEq, Debug)]
#[native_model(id = 1, version = 1)]
#[native_db]
pub struct OsData {
    #[primary_key]
    pub _id: u32,

    #[secondary_key]
    pub _instance_id: InstanceId,

    #[secondary_key]
    pub _timestamp: DbTimestamp,

    /// Distribution name
    pub name: String,
    /// The operating system's family
    pub family: os_info::Type,
    /// The operating system's manufacturer
    pub manufacturer: String,
    /// The operating system's register width in bits
    pub bitness: os_info::Bitness,
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

#[derive(Serialize, Deserialize, PartialEq, Debug)]
#[native_model(id = 3, version = 1)]
#[native_db]
pub struct KernelModuleData {
    #[primary_key]
    pub _id: u32,

    #[secondary_key]
    pub _instance_id: InstanceId,

    #[secondary_key]
    pub _timestamp: DbTimestamp,

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
