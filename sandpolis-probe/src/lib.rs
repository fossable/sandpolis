//! Probe layer for monitoring and managing various device types.
//!
//! Probes are lightweight monitoring endpoints that can be registered on agents
//! to represent devices such as SSH hosts, IPMI-enabled servers, UPS devices,
//! cameras, and more.

use config::{DeviceConfig, ProbeLayerConfig};
use sandpolis_instance::InstanceId;
use serde::{Deserialize, Serialize};
use std::sync::{Arc, LazyLock, OnceLock, RwLock};

pub mod config;
pub mod docker;
pub mod http;
pub mod ipmi;
pub mod libvirt;
pub mod management;
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

/// Devices registered on this instance (populated from config at startup, kept
/// in sync over the management stream).
///
/// This is a global because GUI extension trait methods have no access to layer
/// state when rendering, and the server-side management responder is constructed
/// by a stateless factory.
pub static REGISTERED_DEVICES: LazyLock<Arc<RwLock<Vec<RegisteredDevice>>>> =
    LazyLock::new(Default::default);

/// Hook installed by the top-level `sandpolis` crate (server only) to persist the
/// device list back to `sandpolis.ron`. The probe crate cannot reference the main
/// crate's `Configuration` directly, so persistence is injected here.
static DEVICE_PERSIST: OnceLock<
    Box<dyn Fn(&[RegisteredDevice]) -> anyhow::Result<()> + Send + Sync>,
> = OnceLock::new();

/// Install the persistence hook (see [`DEVICE_PERSIST`]). Idempotent: the first
/// caller wins.
pub fn set_device_persist(
    f: impl Fn(&[RegisteredDevice]) -> anyhow::Result<()> + Send + Sync + 'static,
) {
    let _ = DEVICE_PERSIST.set(Box::new(f));
}

/// Persist the current device list if a hook is installed.
pub fn persist_devices(devices: &[RegisteredDevice]) {
    if let Some(f) = DEVICE_PERSIST.get() {
        if let Err(e) = f(devices) {
            tracing::warn!(error = %e, "Failed to persist probe devices");
        }
    }
}

/// Rebuild the on-disk config from the current device list.
pub fn devices_to_config(devices: &[RegisteredDevice]) -> ProbeLayerConfig {
    ProbeLayerConfig {
        devices: devices.iter().map(|d| d.device.clone()).collect(),
    }
}

/// The probe layer manages device registrations and streaming state.
#[derive(Clone)]
#[cfg_attr(feature = "client-gui", derive(bevy::prelude::Resource))]
pub struct ProbeLayer {
    pub devices: Arc<RwLock<Vec<RegisteredDevice>>>,
}

impl ProbeLayer {
    pub fn new(config: ProbeLayerConfig, gateway: InstanceId) -> Self {
        let devices: Vec<RegisteredDevice> = config
            .devices
            .into_iter()
            .enumerate()
            .map(|(i, device)| RegisteredDevice {
                id: i as u64 + 1,
                gateway,
                device,
                online: false,
                status_message: None,
            })
            .collect();

        *REGISTERED_DEVICES.write().unwrap() = devices;
        Self {
            devices: REGISTERED_DEVICES.clone(),
        }
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

/// A registered device, the runtime counterpart of a [`DeviceConfig`]. Each
/// device maps to exactly one graph node; the protocols it exposes become tabs in
/// its controller.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct RegisteredDevice {
    /// Unique identifier for this device (stable for the process lifetime).
    pub id: u64,

    /// The gateway instance that reaches this device.
    pub gateway: InstanceId,

    /// Name/address plus the per-protocol configuration.
    pub device: DeviceConfig,

    /// Whether the device is currently online/reachable.
    pub online: bool,

    /// Last status message.
    pub status_message: Option<String>,
}

impl RegisteredDevice {
    /// Display name: the configured name, otherwise the IP.
    pub fn display_name(&self) -> String {
        self.device
            .name
            .clone()
            .unwrap_or_else(|| self.device.ip.to_string())
    }
}

impl DeviceConfig {
    /// The protocols this device exposes, in a stable display order.
    pub fn protocols(&self) -> Vec<ProbeType> {
        let mut out = Vec::new();
        if self.rtsp.is_some() {
            out.push(ProbeType::Rtsp);
        }
        if self.onvif.is_some() {
            out.push(ProbeType::Onvif);
        }
        if self.vnc.is_some() {
            out.push(ProbeType::Vnc);
        }
        if self.rdp.is_some() {
            out.push(ProbeType::Rdp);
        }
        if self.ssh.is_some() {
            out.push(ProbeType::Ssh);
        }
        if self.http.is_some() {
            out.push(ProbeType::Http);
        }
        if self.ipmi.is_some() {
            out.push(ProbeType::Ipmi);
        }
        if self.snmp.is_some() {
            out.push(ProbeType::Snmp);
        }
        if self.docker.is_some() {
            out.push(ProbeType::Docker);
        }
        if self.libvirt.is_some() {
            out.push(ProbeType::Libvirt);
        }
        if self.ups.is_some() {
            out.push(ProbeType::Ups);
        }
        if self.wol.is_some() {
            out.push(ProbeType::Wol);
        }
        out
    }

    /// The protocol used to represent the device's graph node icon.
    pub fn primary(&self) -> Option<ProbeType> {
        self.protocols().first().copied()
    }
}
