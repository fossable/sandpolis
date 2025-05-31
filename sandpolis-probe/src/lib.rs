//! This layer extends management functionality out to agent-less "probe"
//! devices.

use sandpolis_core::InstanceId;

// An enumeration of all available communicator types.
pub enum ProbeType {
    Arp,
    Http,
    Ipmi,
    Onvif,
    Rtsp,
    Snmp,
    Ssh,
    Wol,
}

/// Initiate a scan for probes matching the given criteria.
pub struct ProbeScanRequest {
    /// Only scan for probes of this type.
    pub probe_type: ProbeType,

    /// Limit the scan to this network (CIDR)
    pub network: String,
}

pub struct ProbeScanData {
    pub found: Vec<String>,
    pub progress: f32,
    pub estimated_completion: Option<u32>,
}

// message RS_FindSubagents {

//     message SshDevice {

//         // The device's IP address
//         string ip_address = 1;

//         // The device's SSH fingerprint
//         string fingerprint = 2;
//     }

//     message SnmpDevice {

//         // The device's IP address
//         string ip_address = 1;
//     }

//     message IpmiDevice {

//         // The device's IP address
//         string ip_address = 1;
//     }

//     message OnvifDevice {

//         // The device's IP address
//         string ip_address = 1;
//     }

//     message HttpDevice {

//         // The device's IP address
//         string ip_address = 1;

//         // Whether HTTPS is supported
//         bool secure = 2;
//     }

//     message RtspDevice {

//         // The device's IP address
//         string ip_address = 1;
//     }

//     message WolDevice {

//         // The device's IP address
//         string ip_address = 1;

//         // The device's MAC address
//         string mac_address = 2;
//     }

//     repeated SshDevice ssh_device = 1;

//     repeated SnmpDevice snmp_device = 2;

//     repeated IpmiDevice ipmi_device = 3;

//     repeated HttpDevice http_device = 4;

//     repeated OnvifDevice onvif_device = 5;

//     repeated RtspDevice rtsp_device = 6;

//     repeated WolDevice wol_device = 7;
// }

/// Rather than scanning for probes, register one manually.
pub struct RegisterProbeRequest {
    pub ip_address: Option<String>,
    pub mac_address: Option<String>,

    /// The gateway instance
    pub gateway: InstanceId,
}

pub enum RegisterProbeResponse {
    Ok,
}
