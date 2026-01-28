use crate::gui::input::CurrentLayer;
use crate::gui::layer_ext::{get_extension_for_layer, get_layer_extensions};
use bevy::prelude::*;
use bevy_egui::{EguiContexts, egui};
use sandpolis_instance::LayerName;

/// Resource to track layer picker UI state
#[derive(Resource)]
pub struct LayerPickerState {
    pub show: bool,
    pub available_layers: Vec<LayerName>,
    pub search_query: String,
    /// Index of the currently selected item for keyboard navigation
    pub selected_index: usize,
}

impl Default for LayerPickerState {
    fn default() -> Self {
        Self {
            show: false,
            available_layers: get_available_layers(),
            search_query: String::new(),
            selected_index: 0,
        }
    }
}

/// Get list of available layers from registered LayerGuiExtension implementations
fn get_available_layers() -> Vec<LayerName> {
    // Core layers that are always available
    let mut layers = vec![
        LayerName::from("Agent"),
        LayerName::from("Client"),
        LayerName::from("Network"),
        LayerName::from("Server"),
    ];

    // Add layers from registered GUI extensions
    for ext in get_layer_extensions() {
        let layer = ext.layer().clone();
        if !layers.contains(&layer) {
            layers.push(layer);
        }
    }

    layers
}

/// Get emoji icon for a layer
fn get_layer_icon(layer: &LayerName) -> &'static str {
    match layer.name() {
        "Account" => "👤",
        "Agent" => "🤖",
        "Audit" => "📋",
        "Client" => "💻",
        "Deploy" => "🚀",
        "Desktop" => "🖥️",
        "Filesystem" => "📁",
        "Health" => "❤️",
        "Inventory" => "📦",
        "Network" => "🌐",
        "Probe" => "🔍",
        "Server" => "🖧",
        "Shell" => "⌨️",
        "Snapshot" => "📸",
        "Tunnel" => "🔐",
        _ => "❓",
    }
}

/// Get description for a layer
fn get_layer_description(layer: &LayerName) -> &'static str {
    // First check if there's a registered extension with a description
    if let Some(ext) = get_extension_for_layer(layer) {
        return ext.description();
    }

    // Fallback descriptions for core layers without extensions
    match layer.name() {
        "Agent" => "Managed instances running the agent",
        "Client" => "Connected client applications",
        "Network" => "Network topology and connections",
        "Server" => "Server instances in the cluster",
        _ => "No description available",
    }
}

/// Render floating layer picker button (for mobile)
/// Note: This button has been removed - the layer picker now opens via clicking the layer indicator
pub fn render_layer_picker_button(
    _contexts: EguiContexts,
    _picker_state: ResMut<LayerPickerState>,
    _windows: Query<&Window>,
) {
    // Button removed - layer picker now opens via layer indicator click
}

/// Render layer picker panel
pub fn render_layer_picker_panel(
    mut contexts: EguiContexts,
    mut current_layer: ResMut<CurrentLayer>,
    mut picker_state: ResMut<LayerPickerState>,
    windows: Query<&Window>,
    keyboard: Res<ButtonInput<KeyCode>>,
    mouse: Res<ButtonInput<MouseButton>>,
) {
    if !picker_state.show {
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

    let mut panel_hovered = false;

    // Clone available layers to avoid borrow issues
    let available_layers = picker_state.available_layers.clone();
    let search_query = picker_state.search_query.to_lowercase();

    // Filter layers based on search query
    let filtered_layers: Vec<_> = available_layers
        .iter()
        .filter(|layer| {
            if search_query.is_empty() {
                true
            } else {
                layer.name().to_lowercase().contains(&search_query)
            }
        })
        .collect();

    // Handle keyboard navigation before rendering
    let filtered_count = filtered_layers.len();
    if filtered_count > 0 {
        // Clamp selected_index to valid range
        if picker_state.selected_index >= filtered_count {
            picker_state.selected_index = filtered_count.saturating_sub(1);
        }

        // Arrow key navigation
        if keyboard.just_pressed(KeyCode::ArrowDown) {
            picker_state.selected_index = (picker_state.selected_index + 1) % filtered_count;
        }
        if keyboard.just_pressed(KeyCode::ArrowUp) {
            picker_state.selected_index = picker_state
                .selected_index
                .checked_sub(1)
                .unwrap_or(filtered_count - 1);
        }

        // Enter key to select
        if keyboard.just_pressed(KeyCode::Enter) {
            if let Some(layer) = filtered_layers.get(picker_state.selected_index) {
                **current_layer = (*layer).clone();
                picker_state.show = false;
                picker_state.search_query.clear();
                picker_state.selected_index = 0;
                return;
            }
        }
    }

    // Handle Escape key to close the picker
    if keyboard.just_pressed(KeyCode::Escape) {
        picker_state.show = false;
        picker_state.search_query.clear();
        picker_state.selected_index = 0;
        return;
    }

    let response = egui::Window::new("Layer Picker")
        .fixed_pos(panel_pos)
        .title_bar(false)
        .fixed_size([panel_width, panel_height])
        .collapsible(false)
        .resizable(false)
        .show(ctx, |ui| {
            ui.heading("Available Layers");
            ui.separator();

            // Search input (desktop only)
            if !is_mobile {
                ui.horizontal(|ui| {
                    ui.label("🔍");
                    let text_edit = egui::TextEdit::singleline(&mut picker_state.search_query)
                        .hint_text("Search layers...")
                        .desired_width(ui.available_width());

                    let response = ui.add(text_edit);

                    // Auto-focus the search box when the picker opens
                    if picker_state.is_changed() && picker_state.show {
                        response.request_focus();
                    }
                });

                ui.add_space(8.0);
            }

            let mut should_close = false;
            let mut selected_layer: Option<LayerName> = None;

            egui::ScrollArea::vertical().show(ui, |ui| {
                let button_height = if is_mobile { 70.0 } else { 54.0 };
                let button_width = ui.available_width();

                if filtered_layers.is_empty() {
                    ui.label("No matching layers found");
                } else {
                    for (index, layer) in filtered_layers.iter().enumerate() {
                        let is_current = **current_layer == **layer;
                        let is_selected = index == picker_state.selected_index;

                        let fill_color = if is_current {
                            egui::Color32::from_rgb(60, 120, 180)
                        } else if is_selected {
                            egui::Color32::from_rgb(80, 80, 100)
                        } else {
                            egui::Color32::from_gray(40)
                        };

                        let response = ui.allocate_ui_with_layout(
                            egui::vec2(button_width, button_height),
                            egui::Layout::left_to_right(egui::Align::Center),
                            |ui| {
                                let (rect, response) = ui.allocate_exact_size(
                                    egui::vec2(button_width, button_height),
                                    egui::Sense::click(),
                                );

                                if ui.is_rect_visible(rect) {
                                    ui.painter().rect_filled(rect, 4.0, fill_color);

                                    let text_color = egui::Color32::WHITE;
                                    let desc_color = egui::Color32::from_gray(180);

                                    // Layer name with icon
                                    let name_text =
                                        format!("{} {}", get_layer_icon(layer), layer.name());
                                    let name_galley = ui.painter().layout_no_wrap(
                                        name_text,
                                        egui::FontId::proportional(if is_mobile {
                                            18.0
                                        } else {
                                            15.0
                                        }),
                                        text_color,
                                    );

                                    // Description text
                                    let desc_text = get_layer_description(layer);
                                    let desc_galley = ui.painter().layout_no_wrap(
                                        desc_text.to_string(),
                                        egui::FontId::proportional(if is_mobile {
                                            14.0
                                        } else {
                                            11.0
                                        }),
                                        desc_color,
                                    );

                                    let padding = 8.0;
                                    let name_pos =
                                        egui::pos2(rect.left() + padding, rect.top() + padding);
                                    let desc_pos = egui::pos2(
                                        rect.left() + padding,
                                        rect.top() + padding + name_galley.size().y + 4.0,
                                    );

                                    ui.painter().galley(name_pos, name_galley, text_color);
                                    ui.painter().galley(desc_pos, desc_galley, desc_color);
                                }

                                response
                            },
                        );

                        if response.inner.clicked() {
                            selected_layer = Some((*layer).clone());
                            should_close = true;
                        }

                        ui.add_space(if is_mobile { 8.0 } else { 4.0 });
                    }
                }
            });

            // Apply changes after the scroll area
            if let Some(layer) = selected_layer {
                **current_layer = layer;
            }
            if should_close {
                picker_state.show = false;
                picker_state.search_query.clear();
                picker_state.selected_index = 0;
            }
        });

    // Track if the panel was hovered
    if let Some(inner_response) = response {
        panel_hovered = inner_response.response.hovered();
    }

    // Handle touch/click outside to close (mobile)
    if is_mobile && mouse.just_pressed(MouseButton::Left) && !panel_hovered {
        picker_state.show = false;
        picker_state.search_query.clear();
        picker_state.selected_index = 0;
    }
}

/// Handle keyboard shortcut to toggle layer picker
pub fn handle_layer_picker_toggle(
    mut contexts: EguiContexts,
    keyboard: Res<ButtonInput<KeyCode>>,
    mut picker_state: ResMut<LayerPickerState>,
) {
    // Don't handle if egui wants keyboard input
    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };
    if ctx.wants_keyboard_input() {
        return;
    }

    // Toggle layer picker with L key
    if keyboard.just_pressed(KeyCode::KeyL) {
        picker_state.show = !picker_state.show;
        // Clear search query when opening
        if picker_state.show {
            picker_state.search_query.clear();
        }
    }
}
