use serde::Deserialize;
use serde::Serialize;
use std::net::IpAddr;

use crate::rtsp::RtspConfig;

#[derive(Serialize, Deserialize, Debug, Clone, Default)]
pub struct ProbeLayerConfig {
    devices: Vec<DeviceConfig>,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct DeviceConfig {
    ip: IpAddr,
    rtsp: Option<RtspConfig>,
    wol: Option<WolProbeConfig>,
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
