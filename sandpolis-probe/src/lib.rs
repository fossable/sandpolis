//! Probe layer for monitoring and managing various device types.
//!
//! Probes are lightweight monitoring endpoints that can be registered on agents
//! to represent devices such as SSH hosts, IPMI-enabled servers, UPS devices,
//! cameras, and more.

use sandpolis_instance::InstanceId;
use serde::{Deserialize, Serialize};

pub mod config;
pub mod docker;
pub mod http;
pub mod ipmi;
pub mod libvirt;
pub mod onvif;
pub mod rdp;
pub mod rtsp;
pub mod snmp;
pub mod ssh;
pub mod ups;
pub mod vnc;
pub mod wol;

#[cfg(feature = "client-gui")]
pub mod client;

/// An enumeration of all available probe types.
#[derive(Clone, Copy, Debug, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum ProbeType {
    /// Remote Desktop Protocol (Windows)
    Rdp,
    /// Secure Shell
    Ssh,
    /// Uninterruptible Power Supply (via NUT)
    Ups,
    /// Virtual Network Computing
    Vnc,
    /// Wake-on-LAN
    Wol,
    /// HTTP/HTTPS web service
    Http,
    /// Intelligent Platform Management Interface
    Ipmi,
    /// Real Time Streaming Protocol
    Rtsp,
    /// Simple Network Management Protocol
    Snmp,
    /// Open Network Video Interface Forum (IP cameras)
    Onvif,
    /// Docker container engine
    Docker,
    /// libvirt virtualization
    Libvirt,
}

impl ProbeType {
    /// Get a human-readable display name for this probe type.
    pub fn display_name(&self) -> &'static str {
        match self {
            ProbeType::Rdp => "RDP",
            ProbeType::Ssh => "SSH",
            ProbeType::Ups => "UPS",
            ProbeType::Vnc => "VNC",
            ProbeType::Wol => "Wake-on-LAN",
            ProbeType::Http => "HTTP",
            ProbeType::Ipmi => "IPMI",
            ProbeType::Rtsp => "RTSP",
            ProbeType::Snmp => "SNMP",
            ProbeType::Onvif => "ONVIF",
            ProbeType::Docker => "Docker",
            ProbeType::Libvirt => "libvirt",
        }
    }

    /// Get a short description of this probe type.
    pub fn description(&self) -> &'static str {
        match self {
            ProbeType::Rdp => "Windows Remote Desktop Protocol",
            ProbeType::Ssh => "Secure Shell access",
            ProbeType::Ups => "UPS monitoring via Network UPS Tools",
            ProbeType::Vnc => "Virtual Network Computing",
            ProbeType::Wol => "Wake-on-LAN capable device",
            ProbeType::Http => "HTTP/HTTPS web service",
            ProbeType::Ipmi => "Intelligent Platform Management Interface",
            ProbeType::Rtsp => "Real Time Streaming Protocol",
            ProbeType::Snmp => "Simple Network Management Protocol",
            ProbeType::Onvif => "ONVIF-compatible IP camera",
            ProbeType::Docker => "Docker container engine",
            ProbeType::Libvirt => "libvirt virtualization host",
        }
    }

    /// Get all probe types.
    pub fn all() -> &'static [ProbeType] {
        &[
            ProbeType::Rdp,
            ProbeType::Ssh,
            ProbeType::Ups,
            ProbeType::Vnc,
            ProbeType::Wol,
            ProbeType::Http,
            ProbeType::Ipmi,
            ProbeType::Rtsp,
            ProbeType::Snmp,
            ProbeType::Onvif,
            ProbeType::Docker,
            ProbeType::Libvirt,
        ]
    }
}

/// Registered probe data stored in the database.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct RegisteredProbe {
    /// Unique identifier for this probe.
    pub id: u64,

    /// The type of probe.
    pub probe_type: ProbeType,

    /// Human-readable name for this probe.
    pub name: String,

    /// The gateway instance that manages this probe.
    pub gateway: InstanceId,

    /// Type-specific configuration.
    pub config: ProbeConfig,

    /// Whether the probe is currently online/reachable.
    pub online: bool,

    /// Last status message.
    pub status_message: Option<String>,
}

/// Type-specific probe configuration.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub enum ProbeConfig {
    Rdp(RdpProbeConfig),
    Ssh(SshProbeConfig),
    Ups(UpsProbeConfig),
    Vnc(VncProbeConfig),
    Wol(WolProbeConfig),
    Http(HttpProbeConfig),
    Ipmi(IpmiProbeConfig),
    Rtsp(RtspProbeConfig),
    Snmp(SnmpProbeConfig),
    Onvif(OnvifProbeConfig),
    Docker(DockerProbeConfig),
    Libvirt(LibvirtProbeConfig),
}

impl ProbeConfig {
    pub fn probe_type(&self) -> ProbeType {
        match self {
            ProbeConfig::Rdp(_) => ProbeType::Rdp,
            ProbeConfig::Ssh(_) => ProbeType::Ssh,
            ProbeConfig::Ups(_) => ProbeType::Ups,
            ProbeConfig::Vnc(_) => ProbeType::Vnc,
            ProbeConfig::Wol(_) => ProbeType::Wol,
            ProbeConfig::Http(_) => ProbeType::Http,
            ProbeConfig::Ipmi(_) => ProbeType::Ipmi,
            ProbeConfig::Rtsp(_) => ProbeType::Rtsp,
            ProbeConfig::Snmp(_) => ProbeType::Snmp,
            ProbeConfig::Onvif(_) => ProbeType::Onvif,
            ProbeConfig::Docker(_) => ProbeType::Docker,
            ProbeConfig::Libvirt(_) => ProbeType::Libvirt,
        }
    }
}

/// RDP probe configuration.
#[derive(Clone, Debug, Default, Serialize, Deserialize)]
pub struct RdpProbeConfig {
    /// Hostname or IP address.
    pub host: String,
    /// Port (default: 3389).
    pub port: Option<u16>,
    /// Username for authentication.
    pub username: Option<String>,
    /// Password for authentication.
    pub password: Option<String>,
    /// Domain for Windows authentication.
    pub domain: Option<String>,
}

/// SSH probe configuration.
#[derive(Clone, Debug, Default, Serialize, Deserialize)]
pub struct SshProbeConfig {
    /// Hostname or IP address.
    pub host: String,
    /// Port (default: 22).
    pub port: Option<u16>,
    /// Username for authentication.
    pub username: Option<String>,
    /// Password for authentication (if not using key).
    pub password: Option<String>,
    /// Path to private key file.
    pub private_key_path: Option<String>,
    /// SSH fingerprint for verification.
    pub fingerprint: Option<String>,
}

/// UPS probe configuration (via NUT).
#[derive(Clone, Debug, Default, Serialize, Deserialize)]
pub struct UpsProbeConfig {
    /// NUT server hostname.
    pub host: String,
    /// NUT server port (default: 3493).
    pub port: Option<u16>,
    /// UPS name on the NUT server.
    pub ups_name: String,
    /// Username for NUT authentication.
    pub username: Option<String>,
    /// Password for NUT authentication.
    pub password: Option<String>,
}

/// VNC probe configuration.
#[derive(Clone, Debug, Default, Serialize, Deserialize)]
pub struct VncProbeConfig {
    /// Hostname or IP address.
    pub host: String,
    /// Port (default: 5900).
    pub port: Option<u16>,
    /// Password for VNC authentication.
    pub password: Option<String>,
}

/// Wake-on-LAN probe configuration.
#[derive(Clone, Debug, Default, Serialize, Deserialize)]
pub struct WolProbeConfig {
    /// MAC address of the target device.
    pub mac_address: String,
    /// Broadcast address to send the packet to.
    pub broadcast_address: Option<String>,
    /// Port (default: 9).
    pub port: Option<u16>,
    /// Hostname for status checking (optional).
    pub hostname: Option<String>,
}

/// HTTP probe configuration.
#[derive(Clone, Debug, Default, Serialize, Deserialize)]
pub struct HttpProbeConfig {
    /// URL to probe.
    pub url: String,
    /// Expected HTTP status code.
    pub expected_status: Option<u16>,
    /// Request timeout in seconds.
    pub timeout_secs: Option<u32>,
    /// Whether to verify TLS certificates.
    pub verify_tls: Option<bool>,
    /// HTTP method (GET, POST, etc.).
    pub method: Option<String>,
    /// Basic auth username.
    pub username: Option<String>,
    /// Basic auth password.
    pub password: Option<String>,
}

/// IPMI probe configuration.
#[derive(Clone, Debug, Default, Serialize, Deserialize)]
pub struct IpmiProbeConfig {
    /// BMC hostname or IP address.
    pub host: String,
    /// IPMI port (default: 623).
    pub port: Option<u16>,
    /// Username for IPMI authentication.
    pub username: String,
    /// Password for IPMI authentication.
    pub password: String,
    /// IPMI interface type (lanplus, lan, etc.).
    pub interface_type: Option<String>,
}

/// RTSP probe configuration.
#[derive(Clone, Debug, Default, Serialize, Deserialize)]
pub struct RtspProbeConfig {
    /// RTSP URL (rtsp://host:port/path).
    pub url: String,
    /// Username for RTSP authentication.
    pub username: Option<String>,
    /// Password for RTSP authentication.
    pub password: Option<String>,
    /// Transport protocol (UDP, TCP, HTTP).
    pub transport: Option<String>,
}

/// SNMP probe configuration.
#[derive(Clone, Debug, Default, Serialize, Deserialize)]
pub struct SnmpProbeConfig {
    /// Hostname or IP address.
    pub host: String,
    /// SNMP port (default: 161).
    pub port: Option<u16>,
    /// SNMP version (1, 2c, or 3).
    pub version: SnmpVersion,
    /// Community string (for v1/v2c).
    pub community: Option<String>,
    /// Username (for v3).
    pub username: Option<String>,
    /// Auth protocol (for v3).
    pub auth_protocol: Option<String>,
    /// Auth password (for v3).
    pub auth_password: Option<String>,
    /// Privacy protocol (for v3).
    pub priv_protocol: Option<String>,
    /// Privacy password (for v3).
    pub priv_password: Option<String>,
}

/// SNMP version.
#[derive(Clone, Copy, Debug, Default, PartialEq, Eq, Serialize, Deserialize)]
pub enum SnmpVersion {
    #[default]
    V1,
    V2c,
    V3,
}

/// ONVIF probe configuration.
#[derive(Clone, Debug, Default, Serialize, Deserialize)]
pub struct OnvifProbeConfig {
    /// Camera hostname or IP address.
    pub host: String,
    /// HTTP port (default: 80).
    pub port: Option<u16>,
    /// Username for ONVIF authentication.
    pub username: Option<String>,
    /// Password for ONVIF authentication.
    pub password: Option<String>,
    /// Specific profile token to use.
    pub profile_token: Option<String>,
}

/// Docker probe configuration.
#[derive(Clone, Debug, Default, Serialize, Deserialize)]
pub struct DockerProbeConfig {
    /// Docker host URL (e.g., unix:///var/run/docker.sock or tcp://host:2375).
    pub host: String,
    /// TLS CA certificate path.
    pub tls_ca_cert: Option<String>,
    /// TLS client certificate path.
    pub tls_cert: Option<String>,
    /// TLS client key path.
    pub tls_key: Option<String>,
    /// Whether to verify TLS.
    pub tls_verify: Option<bool>,
}

/// libvirt probe configuration.
#[derive(Clone, Debug, Default, Serialize, Deserialize)]
pub struct LibvirtProbeConfig {
    /// libvirt connection URI (e.g., qemu:///system, qemu+ssh://user@host/system).
    pub uri: String,
    /// Username for SSH-based connections.
    pub username: Option<String>,
    /// Private key path for SSH-based connections.
    pub private_key_path: Option<String>,
}

/// Initiate a scan for probes matching the given criteria.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct ProbeScanRequest {
    /// Only scan for probes of this type.
    pub probe_type: ProbeType,

    /// Limit the scan to this network (CIDR).
    pub network: String,
}

/// Progress update during a probe scan.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct ProbeScanData {
    /// Discovered probe addresses/identifiers.
    pub found: Vec<String>,
    /// Scan progress (0.0 to 1.0).
    pub progress: f32,
    /// Estimated seconds until completion.
    pub estimated_completion: Option<u32>,
}

/// Request to register a new probe.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct RegisterProbeRequest {
    /// Human-readable name for the probe.
    pub name: String,

    /// The gateway instance that will manage this probe.
    pub gateway: InstanceId,

    /// Probe configuration.
    pub config: ProbeConfig,
}

/// Response from registering a probe.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub enum RegisterProbeResponse {
    /// Probe registered successfully.
    Ok { probe_id: u64 },
    /// Failed to register probe.
    Failed(String),
}

/// Request to list all probes for a gateway.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct ListProbesRequest {
    /// The gateway instance to list probes for.
    pub gateway: InstanceId,
}

/// Response from listing probes.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct ListProbesResponse {
    /// List of registered probes.
    pub probes: Vec<RegisteredProbe>,
}

/// Request to delete a probe.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct DeleteProbeRequest {
    /// The probe ID to delete.
    pub probe_id: u64,
}

/// Response from deleting a probe.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub enum DeleteProbeResponse {
    Ok,
    NotFound,
    Failed(String),
}

/// Request to test probe connectivity.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct TestProbeRequest {
    /// The probe configuration to test.
    pub config: ProbeConfig,
}

/// Response from testing probe connectivity.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub enum TestProbeResponse {
    /// Connection successful.
    Ok { message: String },
    /// Connection failed.
    Failed { error: String },
}
