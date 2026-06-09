//! Probe layer for monitoring and managing various device types.
//!
//! Probes are lightweight monitoring endpoints that can be registered on agents
//! to represent devices such as SSH hosts, IPMI-enabled servers, UPS devices,
//! cameras, and more.

use sandpolis_instance::InstanceId;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::{Arc, RwLock};

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

/// The probe layer manages probe registrations and streaming state.
#[derive(Clone)]
#[cfg_attr(feature = "client-gui", derive(bevy::prelude::Resource))]
pub struct ProbeLayer {}

impl ProbeLayer {
    pub fn new() -> Self {
        Self {}
    }
}

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
    Rdp(config::RdpProbeConfig),
    Ssh(config::SshProbeConfig),
    Ups(config::UpsProbeConfig),
    Vnc(config::VncProbeConfig),
    Wol(config::WolProbeConfig),
    Http(config::HttpProbeConfig),
    Ipmi(config::IpmiProbeConfig),
    Rtsp(config::RtspProbeConfig),
    Snmp(config::SnmpProbeConfig),
    Onvif(config::OnvifProbeConfig),
    Docker(config::DockerProbeConfig),
    Libvirt(config::LibvirtProbeConfig),
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
