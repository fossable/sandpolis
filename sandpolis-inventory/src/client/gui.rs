//! GUI components for the Inventory layer.
//!
//! Provides the system-information node panel and the layer's client plugin.
//!
//! The bounded quantities — CPU, memory, swap, per-filesystem storage — are all
//! drawn with the shared [`gauge`] control, so "how full is it" reads the same
//! way here as anywhere else in the GUI.

use super::{query_cpu_cores, query_memory, query_mountpoints, query_packages, query_users};
use bevy::prelude::*;
use sandpolis_client::gui::layer_visuals::utilization_tint;
use sandpolis_client::gui::queries::query_instance_metadata;
use sandpolis_client::gui::ui::bind::bind_text;
use sandpolis_client::gui::ui::gauge::{GaugeValue, bind_gauge, gauge};
use sandpolis_client::gui::ui::layer::{LayerClientInfo, RegisterLayerClient};
use sandpolis_client::gui::ui::node_panel::{NodePanel, PanelCtx};
use sandpolis_client::gui::ui::theme::Role;
use sandpolis_client::gui::ui::widgets::{heading, text};
use sandpolis_instance::{InstanceId, InstanceType, LayerName};

/// Width a gauge is given inside a collapsed panel, whose own width is only
/// whatever its content asks for — a percentage-width track would collapse to
/// nothing there.
const SUMMARY_GAUGE_WIDTH: f32 = 170.0;

/// How many filesystems the panel draws a gauge for. Beyond this the list is
/// more scrolling than information; they're sorted largest-first, so what's cut
/// is the least interesting.
const MAX_MOUNTS: usize = 6;

/// The inventory layer's node panel (system information).
pub struct InventoryPanel;

impl NodePanel for InventoryPanel {
    fn build_summary(&self, ctx: &mut PanelCtx) {
        let Some(instance) = ctx.target.instance else {
            return;
        };
        super::subscribe(instance);

        let detailed = ctx.verbosity.is_detailed();
        let theme = ctx.theme;

        ctx.children(|p| {
            p.spawn(Node {
                width: Val::Px(SUMMARY_GAUGE_WIDTH),
                flex_direction: FlexDirection::Column,
                row_gap: Val::Px(theme.metrics.space_xs),
                ..default()
            })
            .with_children(|slot| {
                slot.spawn((
                    gauge(theme, "CPU", cpu_usage(instance)),
                    bind_gauge(move || cpu_usage(instance)),
                ));
                slot.spawn((
                    gauge(theme, "Memory", memory_usage(instance)),
                    bind_gauge(move || memory_usage(instance)),
                ));
                // Zoomed right in there's room for the disks too; at the middle
                // level the two live numbers are the point.
                if detailed {
                    slot.spawn((
                        gauge(theme, "Storage", storage_usage(instance)),
                        bind_gauge(move || storage_usage(instance)),
                    ));
                }
            });
        });
    }

    fn build_detail(&self, ctx: &mut PanelCtx) {
        let Some(instance) = ctx.target.instance else {
            return;
        };
        // Subscribe to live inventory updates for this instance.
        super::subscribe(instance);

        let theme = ctx.theme;
        ctx.children(|p| {
            p.spawn(heading(theme, "CPU"));
            p.spawn((
                gauge(theme, "Utilization", cpu_usage(instance)),
                bind_gauge(move || cpu_usage(instance)),
            ));

            p.spawn(heading(theme, "Memory"));
            p.spawn((
                gauge(theme, "RAM", memory_usage(instance)),
                bind_gauge(move || memory_usage(instance)),
            ));
            p.spawn((
                gauge(theme, "Swap", swap_usage(instance)),
                bind_gauge(move || swap_usage(instance)),
            ));

            p.spawn(heading(theme, "Storage"));
            // One gauge per filesystem, spawned against the mounts known now.
            // A filesystem appearing later needs the panel reopened; disks don't
            // come and go often enough to warrant rebuilding this every frame.
            let mounts = query_mountpoints(instance).unwrap_or_default();
            if mounts.is_empty() {
                p.spawn(text(
                    theme,
                    "No filesystem data",
                    theme.metrics.font_md,
                    Role::TextMuted,
                ));
            }
            for mount in mounts.iter().take(MAX_MOUNTS) {
                let path = mount.path.clone();
                let lookup = path.clone();
                p.spawn((
                    gauge(theme, path, mount_usage_of(mount)),
                    bind_gauge(move || mount_usage(instance, &lookup)),
                ));
            }

            p.spawn(heading(theme, "Users"));
            p.spawn((
                text(theme, "", theme.metrics.font_md, Role::Text),
                bind_text(move || {
                    let users = query_users(instance).unwrap_or_default();
                    if users.is_empty() {
                        return "No user data".into();
                    }
                    let mut names: Vec<String> = users
                        .iter()
                        .map(|u| {
                            u.username
                                .clone()
                                .unwrap_or_else(|| format!("uid {}", u.uid))
                        })
                        .collect();
                    names.sort();
                    format!("{} users — {}", users.len(), names.join(", "))
                }),
            ));

            p.spawn(heading(theme, "Packages"));
            p.spawn((
                text(theme, "", theme.metrics.font_md, Role::Text),
                bind_text(move || {
                    let packages = query_packages(instance).unwrap_or_default();
                    if packages.is_empty() {
                        return "No package data".into();
                    }
                    format!("{} installed packages", packages.len())
                }),
            ));

            p.spawn(text(
                theme,
                format!("Instance: {instance}"),
                theme.metrics.font_sm,
                Role::TextMuted,
            ));
        });
    }
}

/// Mean utilization across an instance's cores.
fn cpu_usage(instance: InstanceId) -> GaugeValue {
    let cores = query_cpu_cores(instance).unwrap_or_default();
    if cores.is_empty() {
        return GaugeValue::new(0.0, "No data");
    }
    let mean = cores.iter().map(|core| core.usage).sum::<f64>() / cores.len() as f64;
    GaugeValue::new(
        mean as f32,
        format!("{:.0}% of {} cores", mean * 100.0, cores.len()),
    )
}

/// An instance's RAM usage.
fn memory_usage(instance: InstanceId) -> GaugeValue {
    let Ok(Some(memory)) = query_memory(instance) else {
        return GaugeValue::new(0.0, "No data");
    };
    let used = memory.total.saturating_sub(memory.free);
    GaugeValue::ratio(
        used,
        memory.total,
        format!(
            "{} / {}",
            format_bytes(used),
            format_bytes(memory.total)
        ),
    )
}

/// An instance's swap usage.
fn swap_usage(instance: InstanceId) -> GaugeValue {
    let Ok(Some(memory)) = query_memory(instance) else {
        return GaugeValue::new(0.0, "No data");
    };
    if memory.swap_total == 0 {
        return GaugeValue::new(0.0, "No swap");
    }
    let used = memory.swap_total.saturating_sub(memory.swap_free);
    GaugeValue::ratio(
        used,
        memory.swap_total,
        format!(
            "{} / {}",
            format_bytes(used),
            format_bytes(memory.swap_total)
        ),
    )
}

/// Usage across every mounted filesystem, for the one-line summary.
fn storage_usage(instance: InstanceId) -> GaugeValue {
    let mounts = query_mountpoints(instance).unwrap_or_default();
    if mounts.is_empty() {
        return GaugeValue::new(0.0, "No data");
    }
    let total: u64 = mounts.iter().map(|mount| mount.total_bytes()).sum();
    let used: u64 = mounts.iter().map(|mount| mount.used_bytes()).sum();
    GaugeValue::ratio(
        used,
        total,
        format!("{} / {}", format_bytes(used), format_bytes(total)),
    )
}

/// Usage of one filesystem, looked up fresh by mount path.
fn mount_usage(instance: InstanceId, path: &str) -> GaugeValue {
    match query_mountpoints(instance)
        .unwrap_or_default()
        .iter()
        .find(|mount| mount.path == path)
    {
        Some(mount) => mount_usage_of(mount),
        None => GaugeValue::new(0.0, "Unmounted"),
    }
}

/// Usage of a filesystem already in hand.
fn mount_usage_of(mount: &crate::os::mountpoint::MountpointData) -> GaugeValue {
    GaugeValue::ratio(
        mount.used_bytes(),
        mount.total_bytes(),
        format!(
            "{} / {}",
            format_bytes(mount.used_bytes()),
            format_bytes(mount.total_bytes())
        ),
    )
}

/// Format a byte count as a human-readable string (GB/MB/KB).
fn format_bytes(bytes: u64) -> String {
    const GB: f64 = 1e9;
    const MB: f64 = 1e6;
    const KB: f64 = 1e3;
    let b = bytes as f64;
    if b >= GB {
        format!("{:.2} GB", b / GB)
    } else if b >= MB {
        format!("{:.2} MB", b / MB)
    } else if b >= KB {
        format!("{:.2} KB", b / KB)
    } else {
        format!("{bytes} B")
    }
}

/// Hardware-type icon for a node while the Inventory layer is active.
///
/// OS type stands in for device class until the hardware inventory reports one
/// directly.
fn node_icon(id: InstanceId) -> &'static str {
    match query_instance_metadata(id).map(|metadata| metadata.os_type) {
        Ok(os_info::Type::Android) => "inventory/mobile.svg",
        Ok(os_info::Type::Windows | os_info::Type::Macos) => "inventory/desktop.svg",
        _ => "inventory/server.svg",
    }
}

/// Tint a node by its memory pressure while the Inventory layer is active.
fn node_tint(id: InstanceId) -> Color {
    match query_memory(id) {
        Ok(Some(memory)) => {
            utilization_tint(memory.total.saturating_sub(memory.free), memory.total)
        }
        _ => Color::WHITE,
    }
}

/// The inventory layer's client plugin.
pub struct InventoryClientPlugin;

impl Plugin for InventoryClientPlugin {
    fn build(&self, app: &mut App) {
        app.register_layer_client(
            LayerClientInfo::new(
                LayerName::from("Inventory"),
                "Hardware and software inventory",
            )
            .with_panel(InventoryPanel)
            .with_visible_instance_types(&[InstanceType::Agent])
            .with_node_icon(node_icon)
            .with_node_tint(node_tint)
            .with_services(),
        );
    }
}
