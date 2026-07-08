//! GUI components for the Inventory layer.
//!
//! Provides the system-information node controller and the layer's client plugin.

use super::{query_memory, query_packages, query_users};
use bevy::prelude::*;
use sandpolis_client::gui::ui::controller::{LayerClientInfo, NodeController, RegisterLayerClient};
use sandpolis_client::gui::ui::scene::{bound_text, text_line};
use sandpolis_client::gui::ui::theme::{Role, Theme};
use sandpolis_instance::{InstanceId, InstanceType, LayerName};

/// The inventory layer's node controller (system information).
pub struct InventoryController;

impl NodeController for InventoryController {
    fn title(&self) -> &str {
        "System Information"
    }

    fn build(&self, commands: &mut Commands, body: Entity, instance: InstanceId, theme: &Theme) {
        // Subscribe to live inventory updates for this instance.
        super::subscribe(instance);

        let font_md = theme.metrics.font_md;
        let font_heading = theme.metrics.font_heading;
        commands.entity(body).apply_scene(bsn! {
            Children [
                // Memory usage
                {vec![text_line(theme, "Memory Usage", Role::Text, font_heading)]},
                {vec![bound_text(theme, Role::Text, font_md, move || {
                    let Ok(Some(m)) = query_memory(instance) else {
                        return "No data".into();
                    };
                    let used = m.total.saturating_sub(m.free);
                    format!(
                        "{} / {} ({:.0}%)",
                        format_bytes(used),
                        format_bytes(m.total),
                        percent(used, m.total),
                    )
                })]},
                // Swap usage
                {vec![text_line(theme, "Swap Usage", Role::Text, font_heading)]},
                {vec![bound_text(theme, Role::Text, font_md, move || {
                    let Ok(Some(m)) = query_memory(instance) else {
                        return "No data".into();
                    };
                    if m.swap_total == 0 {
                        return "No swap".into();
                    }
                    let used = m.swap_total.saturating_sub(m.swap_free);
                    format!(
                        "{} / {} ({:.0}%)",
                        format_bytes(used),
                        format_bytes(m.swap_total),
                        percent(used, m.swap_total),
                    )
                })]},
                // Users
                {vec![text_line(theme, "Users", Role::Text, font_heading)]},
                {vec![bound_text(theme, Role::Text, font_md, move || {
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
                })]},
                // Packages
                {vec![text_line(theme, "Packages", Role::Text, font_heading)]},
                {vec![bound_text(theme, Role::Text, font_md, move || {
                    let packages = query_packages(instance).unwrap_or_default();
                    if packages.is_empty() {
                        return "No package data".into();
                    }
                    format!("{} installed packages", packages.len())
                })]},
                {vec![text_line(
                    theme,
                    format!("Instance: {}", instance),
                    Role::TextMuted,
                    theme.metrics.font_sm,
                )]},
            ]
        });
    }
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
        format!("{} B", bytes)
    }
}

/// Percentage of `used` over `total`, guarding against division by zero.
fn percent(used: u64, total: u64) -> f64 {
    if total == 0 {
        0.0
    } else {
        (used as f64 / total as f64) * 100.0
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
            .with_controller(InventoryController)
            .with_visible_instance_types(&[InstanceType::Server, InstanceType::Agent]),
        );
    }
}
