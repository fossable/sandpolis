//! GUI components for the Health layer.
//!
//! Surfaces systemd unit status through a node controller and client plugin.

use super::query_systemd_units;
use crate::systemd::ActiveState;
use bevy::prelude::*;
use sandpolis_client::gui::ui::bind::bind_text;
use sandpolis_client::gui::ui::controller::{
    LayerClientInfo, NodeController, RegisterLayerClient,
};
use sandpolis_client::gui::ui::theme::{Role, Theme};
use sandpolis_client::gui::ui::widgets::{heading, muted, text};
use sandpolis_instance::{InstanceId, InstanceType, LayerName};

/// The health layer's node controller (service status).
pub struct HealthController;

impl NodeController for HealthController {
    fn title(&self) -> &str {
        "Service Health"
    }

    fn build(&self, commands: &mut Commands, body: Entity, instance: InstanceId, theme: &Theme) {
        // Subscribe to live systemd updates for this instance.
        super::subscribe(instance);

        commands.entity(body).with_children(|p| {
            // Summary of unit states
            p.spawn(heading(theme, "systemd"));
            p.spawn((
                text(theme, "", theme.metrics.font_md, Role::Text),
                bind_text(move || {
                    let units = query_systemd_units(instance).unwrap_or_default();
                    if units.is_empty() {
                        return "No unit data".into();
                    }
                    let failed = units
                        .iter()
                        .filter(|u| u.active_state == ActiveState::Failed)
                        .count();
                    let active = units
                        .iter()
                        .filter(|u| u.active_state == ActiveState::Active)
                        .count();
                    format!("{} units — {} active, {} failed", units.len(), active, failed)
                }),
            ));

            // Failed units (most actionable)
            p.spawn(heading(theme, "Failed Units"));
            p.spawn((
                text(theme, "", theme.metrics.font_md, Role::Text),
                bind_text(move || {
                    let units = query_systemd_units(instance).unwrap_or_default();
                    let failed: Vec<String> = units
                        .iter()
                        .filter(|u| u.active_state == ActiveState::Failed)
                        .map(|u| u.name.clone())
                        .collect();
                    if failed.is_empty() {
                        "None".into()
                    } else {
                        failed.join("\n")
                    }
                }),
            ));

            p.spawn(muted(
                theme,
                format!("Instance: {}", instance),
                theme.metrics.font_sm,
            ));
        });
    }
}

/// The health layer's client plugin.
pub struct HealthClientPlugin;

impl Plugin for HealthClientPlugin {
    fn build(&self, app: &mut App) {
        app.register_layer_client(
            LayerClientInfo::new(LayerName::from("Health"), "Service and host health")
                .with_controller(HealthController)
                .with_visible_instance_types(&[InstanceType::Agent]),
        );
    }
}
