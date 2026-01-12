use super::{
    CurrentLayer,
    components::{LayerIndicatorState, MinimapViewport},
    layer_switcher::LayerSwitcherState,
};
use crate::Layer;
use bevy::prelude::*;
use bevy_egui::{EguiContexts, egui};

/// Get the SVG file URI for a layer
fn get_layer_svg_uri(layer: &Layer) -> String {
    let relative_path = match layer {
        #[cfg(feature = "layer-account")]
        Layer::Account => "layers/Account.svg",
        Layer::Agent => "layers/Network.svg",
        #[cfg(feature = "layer-audit")]
        Layer::Audit => "layers/Audit.svg",
        Layer::Client => "layers/Network.svg",
        #[cfg(feature = "layer-deploy")]
        Layer::Deploy => "layers/Deploy.svg",
        #[cfg(feature = "layer-desktop")]
        Layer::Desktop => "layers/Desktop.svg",
        #[cfg(feature = "layer-filesystem")]
        Layer::Filesystem => "layers/Filesystem.svg",
        #[cfg(feature = "layer-health")]
        Layer::Health => "layers/Health.svg",
        #[cfg(feature = "layer-inventory")]
        Layer::Inventory => "layers/Inventory.svg",
        Layer::Network => "layers/Network.svg",
        #[cfg(feature = "layer-probe")]
        Layer::Probe => "layers/Probe.svg",
        Layer::Server => "layers/Network.svg",
        #[cfg(feature = "layer-shell")]
        Layer::Shell => "layers/Shell.svg",
        #[cfg(feature = "layer-snapshot")]
        Layer::Snapshot => "layers/Snapshot.svg",
        #[cfg(feature = "layer-tunnel")]
        Layer::Tunnel => "layers/Tunnel.svg",
    };

    // Use file:// URI with absolute path from current working directory
    // The app runs from the sandpolis/ directory, assets are in ../sandpolis-client/assets
    let current_dir = std::env::current_dir()
        .unwrap_or_else(|_| std::path::PathBuf::from("."));
    let asset_path = current_dir.join("../sandpolis-client/assets").join(relative_path);

    // Canonicalize to get absolute path and convert to file:// URI
    if let Ok(canonical) = asset_path.canonicalize() {
        format!("file://{}", canonical.display())
    } else {
        // Fallback to relative path if canonicalization fails
        format!("../sandpolis-client/assets/{}", relative_path)
    }
}

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

            let layer_name = layer_display_name(&current_layer);
            let svg_uri = get_layer_svg_uri(&current_layer);

            // Calculate button width to match minimap width
            let button_width = minimap_viewport.width;

            // Create horizontal layout with SVG icon and button
            let response = ui.horizontal(|ui| {
                ui.spacing_mut().item_spacing = egui::vec2(4.0, 0.0);

                // SVG icon with size hint for proper rendering
                let icon_size = egui::vec2(24.0, 24.0);
                ui.add(
                    egui::Image::new(svg_uri)
                        .fit_to_exact_size(icon_size)
                        .tint(egui::Color32::from_rgba_premultiplied(220, 240, 255, text_alpha))
                );

                // Button with layer name - adjust width to account for icon
                let button_response = ui.add(
                    egui::Button::new(
                        egui::RichText::new(layer_name)
                            .size(16.0)
                            .color(egui::Color32::from_rgba_premultiplied(220, 240, 255, text_alpha))
                    )
                    .fill(egui::Color32::from_rgba_unmultiplied(30, 50, 70, button_alpha))
                    .stroke(egui::Stroke::new(
                        1.5,
                        egui::Color32::from_rgba_unmultiplied(80, 140, 200, text_alpha),
                    ))
                    .rounding(6.0)
                    .min_size(egui::vec2(button_width - icon_size.x - 8.0, 32.0))
                );

                button_response
            }).inner;

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

/// Get an icon/emoji for a layer
pub fn get_layer_icon(layer: &Layer) -> &'static str {
    match layer {
        #[cfg(feature = "layer-account")]
        Layer::Account => "👤",
        Layer::Agent => "🤖",
        #[cfg(feature = "layer-audit")]
        Layer::Audit => "🔍",
        Layer::Client => "💻",
        #[cfg(feature = "layer-deploy")]
        Layer::Deploy => "🚀",
        #[cfg(feature = "layer-desktop")]
        Layer::Desktop => "🖥️",
        #[cfg(feature = "layer-filesystem")]
        Layer::Filesystem => "📁",
        #[cfg(feature = "layer-health")]
        Layer::Health => "❤️",
        #[cfg(feature = "layer-inventory")]
        Layer::Inventory => "📦",
        Layer::Network => "🌐",
        #[cfg(feature = "layer-probe")]
        Layer::Probe => "📡",
        Layer::Server => "🖧",
        #[cfg(feature = "layer-shell")]
        Layer::Shell => "⌨️",
        #[cfg(feature = "layer-snapshot")]
        Layer::Snapshot => "📸",
        #[cfg(feature = "layer-tunnel")]
        Layer::Tunnel => "🔗",
    }
}
