use super::{
    CurrentLayer,
    components::{LayerIndicatorState, MinimapViewport},
    layer_switcher::LayerSwitcherState,
};
use crate::Layer;
use bevy::prelude::*;
use bevy_egui::{EguiContexts, egui};

/// Render the layer indicator above the minimap
/// Shows the currently active layer permanently with fade in/fade out on layer changes
/// Clicking the indicator opens the layer switcher
pub fn render_layer_indicator(
    mut contexts: EguiContexts,
    current_layer: Res<CurrentLayer>,
    mut indicator_state: ResMut<LayerIndicatorState>,
    mut switcher_state: ResMut<LayerSwitcherState>,
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

    let Ok(window) = windows.single() else {
        return;
    };
    let window_size = Vec2::new(window.width(), window.height());

    // Calculate fade animation based on timer
    let elapsed = indicator_state.show_timer.elapsed_secs();

    // Fade in: 0.0 to 1.0 over first 0.4 seconds when layer changes
    let fade_in_duration = 0.4;
    let alpha = if elapsed < fade_in_duration {
        (elapsed / fade_in_duration).min(1.0)
    } else {
        1.0 // Stay fully visible after fade in completes
    };

    // Position above minimap (static position, no jiggling)
    let indicator_pos = egui::Pos2::new(
        window_size.x - minimap_viewport.width - minimap_viewport.bottom_right_offset.x,
        window_size.y - minimap_viewport.height - minimap_viewport.bottom_right_offset.y - 40.0,
    );

    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };

    egui::Window::new("Layer Indicator")
        .title_bar(false)
        .resizable(false)
        .movable(false)
        .fixed_pos(indicator_pos)
        .frame(egui::Frame::none())
        .show(ctx, |ui| {
            // Custom styled button with gradient-like appearance
            let button_alpha = (alpha * 200.0) as u8;
            let text_alpha = (alpha * 255.0) as u8;

            let response = ui.add(
                egui::Button::new(
                    egui::RichText::new(format!("▶ {:?}", *current_layer))
                        .size(16.0)
                        .color(egui::Color32::from_rgba_premultiplied(220, 240, 255, text_alpha))
                )
                .fill(egui::Color32::from_rgba_unmultiplied(30, 50, 70, button_alpha))
                .stroke(egui::Stroke::new(
                    1.5,
                    egui::Color32::from_rgba_unmultiplied(80, 140, 200, text_alpha),
                ))
                .rounding(6.0)
                .min_size(egui::vec2(140.0, 32.0))
            );

            if response.clicked() {
                switcher_state.show = !switcher_state.show;
            }

            // Show hover hint
            if response.hovered() {
                response.on_hover_text("Click to switch layers");
            }
        });
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
