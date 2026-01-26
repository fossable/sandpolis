use serde::{Deserialize, Serialize};

/// Request that the boot agent be started.
#[derive(Serialize, Deserialize)]
pub struct LaunchBootAgentRequest {
    /// The UUID of the partition containing the boot agent
    pub target_uuid: String,
}

#[derive(Serialize, Deserialize)]
pub enum LaunchBootAgentResponse {
    Ok,
    AccessDenied,
}

/// Request a boot agent be uninstalled from the system.
#[derive(Serialize, Deserialize)]
pub struct UninstallBootAgentRequest {
    /// The UUID of the partition containing the boot agent
    pub target_uuid: String,
}

#[derive(Serialize, Deserialize)]
pub enum UninstallBootAgentResponse {
    Ok,
    AccessDenied,
}

/// Scan for installed boot agents.
pub struct ScanBootAgentRequest {}

// Response listing boot agent installations.
//
// Sources      : client, server
// Destinations : agent
//
// message RS_FindBootAgents {

//     message BootAgentInstallation {

//         // The partition UUID
//         repeated string partition_uuid = 1;

//         // The partition size in bytes
//         int64 size = 2;

//         // The boot agent's version string
//         string version = 3;
//     }

//     repeated BootAgentInstallation installation = 1;
// }

/// Request a boot agent be installed on the system.
#[derive(Serialize, Deserialize)]
pub struct InstallBootAgentRequest {
    /// The UUID of the target partition
    pub partition_uuid: String,

    /// The MAC address of the network interface to use for connections
    pub interface_mac: String,

    /// Whether DHCP will be used
    pub use_dhcp: bool,

    /// A static IP address as an alternative to DHCP
    pub static_ip: String,

    /// The netmask corresponding to the static IP
    pub netmask: String,

    /// The gateway IP
    pub gateway_ip: String,
}

#[derive(Serialize, Deserialize)]
pub enum InstallBootAgentResponse {
    Ok,
    AccessDenied,
}

// Response listing boot agent installation candidates.
//
// Sources      : agent
// Destinations : client, server
// Request      : RQ_FindBootAgents
//
// message RS_FindBootAgentCandidates {

//     message DeviceCandidate {

//         // The device's UUID
//         string device_uuid = 1;

//         // The size of the device in bytes
//         int64 device_size = 2;

//         // The device name
//         string device_name = 3;
//     }

//     message PartitionCandidate {

//         // The partition's UUID
//         string partition_uuid = 1;

//         // The size of the partition in bytes
//         int64 partition_size = 2;

//         // The device name upon which the partition exists
//         string device_name = 3;

//         // The amount of free space remaining on the device's ESP
//         int64 esp_free_space = 4;
//     }

//     repeated DeviceCandidate device_candidate = 1;

//     repeated PartitionCandidate partition_candidate = 2;
// }
