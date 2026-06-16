//! GUI components for the Inventory layer.
//!
//! Provides the system-information node controller and the layer's client plugin.

use bevy::prelude::*;
use sandpolis_client::gui::ui::bind::bind_text;
use sandpolis_client::gui::ui::controller::{
    LayerClientInfo, NodeController, RegisterLayerClient,
};
use sandpolis_client::gui::ui::theme::{Role, Theme};
use sandpolis_client::gui::ui::widgets::{heading, muted, text};
use sandpolis_instance::{InstanceId, InstanceType, LayerName};

/// Hardware information.
#[derive(Clone, Debug, Default)]
pub struct HardwareInfo {
    pub cpu_model: Option<String>,
    pub cpu_cores: Option<u32>,
    pub memory_total: Option<u64>,
}

/// Memory statistics.
#[derive(Clone, Debug, Default)]
pub struct MemoryStats {
    pub total: u64,
    pub used: u64,
    pub free: u64,
}

/// Filesystem usage statistics.
#[derive(Clone, Debug, Default)]
pub struct FilesystemUsage {
    pub total: u64,
    pub used: u64,
    pub free: u64,
}

/// Network statistics.
#[derive(Clone, Debug, Default)]
pub struct NetworkStats {
    pub latency_ms: Option<u32>,
    pub throughput_bps: Option<u64>,
}

/// Instance metadata.
#[derive(Clone, Debug, Default)]
pub struct InstanceMetadata {
    pub os_type: String,
    pub hostname: Option<String>,
}

/// Package information.
#[derive(Clone, Debug)]
pub struct PackageInfo {
    pub name: String,
    pub version: String,
}

/// Query hardware information for an instance.
pub fn query_hardware_info(_id: InstanceId) -> anyhow::Result<HardwareInfo> {
    // TODO: Query from inventory resident
    Ok(HardwareInfo::default())
}

/// Query instance metadata.
pub fn query_instance_metadata(_id: InstanceId) -> anyhow::Result<InstanceMetadata> {
    // TODO: Query from database
    Ok(InstanceMetadata::default())
}

/// Query memory statistics.
pub fn query_memory_stats(_id: InstanceId) -> anyhow::Result<MemoryStats> {
    // TODO: Query from inventory resident
    Ok(MemoryStats::default())
}

/// Query filesystem usage.
pub fn query_filesystem_usage(_id: InstanceId) -> anyhow::Result<FilesystemUsage> {
    // TODO: Query from inventory resident
    Ok(FilesystemUsage::default())
}

/// Query network statistics.
pub fn query_network_stats(_id: InstanceId) -> anyhow::Result<NetworkStats> {
    // TODO: Query from network layer
    Ok(NetworkStats::default())
}

/// Query installed packages.
pub fn query_packages(_id: InstanceId) -> anyhow::Result<Vec<PackageInfo>> {
    // TODO: Query from inventory resident
    Ok(vec![])
}

/// The inventory layer's node controller (system information).
pub struct InventoryController;

impl NodeController for InventoryController {
    fn title(&self) -> &str {
        "System Information"
    }

    fn build(&self, commands: &mut Commands, body: Entity, instance: InstanceId, theme: &Theme) {
        commands.entity(body).with_children(|p| {
            // Hardware
            p.spawn(heading(theme, "Hardware"));
            p.spawn((
                text(theme, "", theme.metrics.font_md, Role::Text),
                bind_text(move || {
                    let info = query_hardware_info(instance).unwrap_or_default();
                    let cpu = info.cpu_model.unwrap_or_else(|| "Unknown".into());
                    let cores = info
                        .cpu_cores
                        .map(|c| format!(", {} cores", c))
                        .unwrap_or_default();
                    let ram = info
                        .memory_total
                        .map(|m| format!(" — {:.2} GB RAM", m as f64 / 1e9))
                        .unwrap_or_default();
                    format!("CPU: {}{}{}", cpu, cores, ram)
                }),
            ));

            // Operating system
            p.spawn(heading(theme, "Operating System"));
            p.spawn((
                text(theme, "", theme.metrics.font_md, Role::Text),
                bind_text(move || {
                    let m = query_instance_metadata(instance).unwrap_or_default();
                    let host = m.hostname.unwrap_or_else(|| "unknown".into());
                    let os = if m.os_type.is_empty() {
                        "Unknown".into()
                    } else {
                        m.os_type
                    };
                    format!("{} ({})", host, os)
                }),
            ));

            // Memory usage
            p.spawn(heading(theme, "Memory Usage"));
            p.spawn((
                text(theme, "", theme.metrics.font_md, Role::Text),
                bind_text(move || usage_line(query_memory_stats(instance).map(|m| (m.used, m.total)))),
            ));

            // Storage usage
            p.spawn(heading(theme, "Storage Usage"));
            p.spawn((
                text(theme, "", theme.metrics.font_md, Role::Text),
                bind_text(move || {
                    usage_line(query_filesystem_usage(instance).map(|u| (u.used, u.total)))
                }),
            ));

            // Network
            p.spawn(heading(theme, "Network"));
            p.spawn((
                text(theme, "", theme.metrics.font_md, Role::Text),
                bind_text(move || {
                    let s = query_network_stats(instance).unwrap_or_default();
                    match (s.latency_ms, s.throughput_bps) {
                        (Some(l), Some(t)) => {
                            format!("{} ms, {:.2} Mbps", l, t as f64 / 1e6)
                        }
                        (Some(l), None) => format!("{} ms", l),
                        _ => "No network statistics".into(),
                    }
                }),
            ));

            p.spawn(muted(theme, format!("Instance: {}", instance), theme.metrics.font_sm));
        });
    }
}

/// Format a used/total byte pair as "X.X GB / Y.Y GB (Z%)".
fn usage_line(stats: anyhow::Result<(u64, u64)>) -> String {
    match stats {
        Ok((used, total)) if total > 0 => {
            let percent = (used as f64 / total as f64) * 100.0;
            format!(
                "{:.2} GB / {:.2} GB ({:.0}%)",
                used as f64 / 1e9,
                total as f64 / 1e9,
                percent
            )
        }
        _ => "No data".into(),
    }
}

/// The inventory layer's client plugin.
pub struct InventoryClientPlugin;

impl Plugin for InventoryClientPlugin {
    fn build(&self, app: &mut App) {
        app.register_layer_client(
            LayerClientInfo::new(LayerName::from("Inventory"), "Hardware and software inventory")
                .with_controller(InventoryController)
                .with_visible_instance_types(&[InstanceType::Server, InstanceType::Agent]),
        );
    }
}
