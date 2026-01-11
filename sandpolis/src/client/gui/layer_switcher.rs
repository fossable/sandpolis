use super::CurrentLayer;
use crate::Layer;
use bevy::prelude::*;
use bevy_egui::{egui, EguiContexts};

/// Resource to track layer switcher UI state
#[derive(Resource)]
pub struct LayerSwitcherState {
    pub show: bool,
    pub available_layers: Vec<Layer>,
}

impl Default for LayerSwitcherState {
    fn default() -> Self {
        Self {
            show: false,
            available_layers: get_available_layers(),
        }
    }
}

/// Get list of available layers based on enabled features
fn get_available_layers() -> Vec<Layer> {
    let mut layers = vec![
        Layer::Agent,
        Layer::Client,
        Layer::Network,
        Layer::Server,
    ];

    #[cfg(feature = "layer-account")]
    layers.push(Layer::Account);

    #[cfg(feature = "layer-audit")]
    layers.push(Layer::Audit);

    #[cfg(feature = "layer-deploy")]
    layers.push(Layer::Deploy);

    #[cfg(feature = "layer-desktop")]
    layers.push(Layer::Desktop);

    #[cfg(feature = "layer-filesystem")]
    layers.push(Layer::Filesystem);

    #[cfg(feature = "layer-health")]
    layers.push(Layer::Health);

    #[cfg(feature = "layer-inventory")]
    layers.push(Layer::Inventory);

    #[cfg(feature = "layer-probe")]
    layers.push(Layer::Probe);

    #[cfg(feature = "layer-shell")]
    layers.push(Layer::Shell);

    #[cfg(feature = "layer-snapshot")]
    layers.push(Layer::Snapshot);

    #[cfg(feature = "layer-tunnel")]
    layers.push(Layer::Tunnel);

    layers
}

/// Get emoji icon for a layer
fn get_layer_icon(layer: &Layer) -> &'static str {
    match layer {
        #[cfg(feature = "layer-account")]
        Layer::Account => "👤",
        Layer::Agent => "🤖",
        #[cfg(feature = "layer-audit")]
        Layer::Audit => "📋",
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
        Layer::Probe => "🔍",
        Layer::Server => "🖧",
        #[cfg(feature = "layer-shell")]
        Layer::Shell => "⌨️",
        #[cfg(feature = "layer-snapshot")]
        Layer::Snapshot => "📸",
        #[cfg(feature = "layer-tunnel")]
        Layer::Tunnel => "🔐",
    }
}

/// Render floating layer switcher button (for mobile)
/// Note: This button has been removed - the layer switcher now opens via clicking the layer indicator
pub fn render_layer_switcher_button(
    _contexts: EguiContexts,
    _switcher_state: ResMut<LayerSwitcherState>,
    _windows: Query<&Window>,
) {
    // Button removed - layer switcher now opens via layer indicator click
}

/// Render layer switcher panel
pub fn render_layer_switcher_panel(
    mut contexts: EguiContexts,
    mut current_layer: ResMut<CurrentLayer>,
    mut switcher_state: ResMut<LayerSwitcherState>,
    windows: Query<&Window>,
) {
    if !switcher_state.show {
        return;
    }

    let Ok(window) = windows.single() else {
        return;
    };

    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };

    let window_size = Vec2::new(window.width(), window.height());
    let is_mobile = window_size.x < 800.0;

    // Panel dimensions
    let panel_width = if is_mobile {
        window_size.x * 0.85
    } else {
        300.0
    };
    let panel_height = if is_mobile {
        window_size.y * 0.7
    } else {
        400.0
    };

    // Center the panel
    let panel_pos = egui::Pos2::new(
        (window_size.x - panel_width) / 2.0,
        (window_size.y - panel_height) / 2.0,
    );

    egui::Window::new("Select Layer")
        .fixed_pos(panel_pos)
        .fixed_size([panel_width, panel_height])
        .collapsible(false)
        .resizable(false)
        .show(ctx, |ui| {
            ui.heading("Available Layers");
            ui.separator();

            // Clone available layers to avoid borrow issues
            let available_layers = switcher_state.available_layers.clone();
            let mut should_close = false;
            let mut selected_layer: Option<Layer> = None;

            egui::ScrollArea::vertical().show(ui, |ui| {
                let button_height = if is_mobile { 50.0 } else { 40.0 };

                for layer in &available_layers {
                    let is_current = **current_layer == *layer;

                    let button_text = egui::RichText::new(format!(
                        "{} {:?}",
                        get_layer_icon(layer),
                        layer
                    ))
                    .size(if is_mobile { 18.0 } else { 16.0 });

                    let button = egui::Button::new(button_text)
                        .fill(if is_current {
                            egui::Color32::from_rgb(60, 120, 180)
                        } else {
                            egui::Color32::from_gray(40)
                        })
                        .min_size(egui::vec2(panel_width - 40.0, button_height));

                    if ui.add(button).clicked() && !is_current {
                        selected_layer = Some(*layer);
                        should_close = true;
                    }

                    ui.add_space(if is_mobile { 8.0 } else { 4.0 });
                }
            });

            // Apply changes after the scroll area
            if let Some(layer) = selected_layer {
                **current_layer = layer;
            }
            if should_close {
                switcher_state.show = false;
            }

            ui.separator();

            // Close button at bottom
            ui.with_layout(egui::Layout::bottom_up(egui::Align::Center), |ui| {
                if ui
                    .add_sized(
                        [panel_width - 40.0, if is_mobile { 45.0 } else { 35.0 }],
                        egui::Button::new(
                            egui::RichText::new("Close").size(if is_mobile { 18.0 } else { 16.0 }),
                        ),
                    )
                    .clicked()
                {
                    switcher_state.show = false;
                }
            });
        });
}

/// Handle keyboard shortcut to toggle layer switcher
/// Note: This function is kept for potential future keyboard shortcuts
/// The layer switcher is now opened by clicking the layer indicator
pub fn handle_layer_switcher_toggle(
    _keyboard: Res<ButtonInput<KeyCode>>,
    _switcher_state: ResMut<LayerSwitcherState>,
) {
    // L keybinding removed - layer switcher now opens via layer indicator click
}
