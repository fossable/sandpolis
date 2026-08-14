//! GUI components for the Health layer.
//!
//! Surfaces systemd unit status through a node panel and client plugin.

use super::{SystemdUnitInfo, query_systemd_units};
use crate::systemd::ActiveState;
use bevy::prelude::*;
use sandpolis_client::gui::ui::bind::bind_text;
use sandpolis_client::gui::ui::layer::{LayerClientInfo, RegisterLayerClient};
use sandpolis_client::gui::ui::node_panel::{NodePanel, PanelCtx};
use sandpolis_client::gui::ui::theme::Role;
use sandpolis_client::gui::ui::widgets::{heading, text};
use sandpolis_instance::{InstanceType, LayerName};

/// The health layer's node panel (service status).
pub struct HealthPanel;

impl NodePanel for HealthPanel {
    fn build_summary(&self, ctx: &mut PanelCtx) {
        let Some(instance) = ctx.target.instance else {
            return;
        };
        super::subscribe(instance);

        let detailed = ctx.verbosity.is_detailed();
        let theme = ctx.theme;

        ctx.children(|p| {
            p.spawn((
                text(theme, "", theme.metrics.font_sm, Role::TextMuted),
                bind_text(move || {
                    let units = query_systemd_units(instance).unwrap_or_default();
                    if units.is_empty() {
                        return "No unit data".into();
                    }
                    let failed = count(&units, ActiveState::Failed);
                    let summary = format!("{} units — {} failed", units.len(), failed);
                    if !detailed || failed == 0 {
                        return summary;
                    }
                    // Zoomed right in there's room to name what's actually
                    // broken, which is the only part anyone acts on.
                    let names: Vec<&str> = units
                        .iter()
                        .filter(|unit| unit.active_state == ActiveState::Failed)
                        .map(|unit| unit.name.as_str())
                        .take(3)
                        .collect();
                    format!("{}\n{}", summary, names.join(", "))
                }),
            ));
        });
    }

    fn build_detail(&self, ctx: &mut PanelCtx) {
        let Some(instance) = ctx.target.instance else {
            return;
        };
        // Subscribe to live systemd updates for this instance.
        super::subscribe(instance);

        let theme = ctx.theme;
        ctx.children(|p| {
            p.spawn(heading(theme, "systemd"));
            p.spawn((
                text(theme, "", theme.metrics.font_md, Role::Text),
                bind_text(move || {
                    let units = query_systemd_units(instance).unwrap_or_default();
                    if units.is_empty() {
                        return "No unit data".into();
                    }
                    format!(
                        "{} units — {} active, {} failed",
                        units.len(),
                        count(&units, ActiveState::Active),
                        count(&units, ActiveState::Failed),
                    )
                }),
            ));

            // Failed units (most actionable).
            p.spawn(heading(theme, "Failed Units"));
            p.spawn((
                text(theme, "", theme.metrics.font_md, Role::Text),
                bind_text(move || {
                    let units = query_systemd_units(instance).unwrap_or_default();
                    let failed: Vec<String> = units
                        .iter()
                        .filter(|unit| unit.active_state == ActiveState::Failed)
                        .map(|unit| unit.name.clone())
                        .collect();
                    if failed.is_empty() {
                        "None".into()
                    } else {
                        failed.join("\n")
                    }
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

/// How many of `units` are in the given state.
fn count(units: &[SystemdUnitInfo], state: ActiveState) -> usize {
    units
        .iter()
        .filter(|unit| unit.active_state == state)
        .count()
}

/// The health layer's client plugin.
pub struct HealthClientPlugin;

impl Plugin for HealthClientPlugin {
    fn build(&self, app: &mut App) {
        app.register_layer_client(
            LayerClientInfo::new(LayerName::from("Health"), "Service and host health")
                .with_panel(HealthPanel)
                .with_visible_instance_types(&[InstanceType::Agent])
                .with_services(),
        );
    }
}
