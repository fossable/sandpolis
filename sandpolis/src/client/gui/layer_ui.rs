use super::{
    CurrentLayer,
    components::{LayerIndicatorState, MinimapViewport},
};
use crate::Layer;
use bevy::prelude::*;
use bevy_egui::{EguiContexts, egui};

/// Render the layer indicator above the minimap
/// Shows the currently active layer for a few seconds after switching
pub fn render_layer_indicator(
    mut contexts: EguiContexts,
    current_layer: Res<CurrentLayer>,
    mut indicator_state: ResMut<LayerIndicatorState>,
    minimap_viewport: Res<MinimapViewport>,
    time: Res<Time>,
    windows: Query<&Window>,
) {
    // Tick the timer
    indicator_state.show_timer.tick(time.delta());

    // Reset timer when layer changes
    if current_layer.is_changed() && !current_layer.is_added() {
        indicator_state.show_timer.reset();
    }

    // Only show if timer hasn't finished
    if !indicator_state.show_timer.just_finished() {
        let Ok(window) = windows.single() else {
            return;
        };
        let window_size = Vec2::new(window.width(), window.height());

        // Position above minimap
        let indicator_pos = egui::Pos2::new(
            window_size.x - minimap_viewport.width - minimap_viewport.bottom_right_offset.x,
            window_size.y - minimap_viewport.height - minimap_viewport.bottom_right_offset.y - 40.0, // 40 pixels above minimap
        );

        let Ok(ctx) = contexts.ctx_mut() else {
            return;
        };

        egui::Window::new("Layer Indicator")
            .title_bar(false)
            .resizable(false)
            .movable(false)
            .fixed_pos(indicator_pos)
            .show(ctx, |ui| {
                ui.horizontal(|ui| {
                    ui.label(egui::RichText::new("Layer:").size(14.0));
                    ui.label(
                        egui::RichText::new(format!("{:?}", *current_layer))
                            .size(16.0)
                            .color(egui::Color32::from_rgb(100, 200, 255)),
                    );
                });
            });
    }
}

/// Get a display-friendly name for a layer
#[allow(dead_code)]
pub fn layer_display_name(layer: &Layer) -> &'static str {
    match layer {
        #[cfg(feature = "layer-account")]
        Layer::Account => "Account",
        Layer::Agent => "Agent",
        #[cfg(feature = "layer-audit")]
        Layer::Audit => "Audit",
        Layer::Client => "Client",
        #[cfg(feature = "layer-deploy")]
        Layer::Deploy => "Deploy",
        #[cfg(feature = "layer-desktop")]
        Layer::Desktop => "Desktop",
        #[cfg(feature = "layer-filesystem")]
        Layer::Filesystem => "Filesystem",
        #[cfg(feature = "layer-health")]
        Layer::Health => "Health",
        #[cfg(feature = "layer-inventory")]
        Layer::Inventory => "Inventory",
        Layer::Network => "Network",
        #[cfg(feature = "layer-probe")]
        Layer::Probe => "Probe",
        Layer::Server => "Server",
        #[cfg(feature = "layer-shell")]
        Layer::Shell => "Shell",
        #[cfg(feature = "layer-snapshot")]
        Layer::Snapshot => "Snapshot",
        #[cfg(feature = "layer-tunnel")]
        Layer::Tunnel => "Tunnel",
    }
}
