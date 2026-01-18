use crate::gui::queries;
use crate::gui::{CurrentLayer, NodeEntity, controller::{NodeControllerState, ControllerType}};
use crate::gui::WorldView;
use sandpolis_core::Layer;
use bevy::prelude::*;
use bevy_egui::{EguiContexts, egui};
use sandpolis_core::InstanceId;

/// Zoom threshold above which the minimal preview is shown.
/// At zoom level 1.0, preview is full. Above this value (more zoomed out), preview becomes minimal.
const MINIMAL_PREVIEW_ZOOM_THRESHOLD: f32 = 1.2;

/// Component tracking NodePreview state for each node
#[derive(Component)]
pub struct NodePreview {
    pub show: bool,
    pub width: f32,
    pub height: f32,
}

impl NodePreview {
    /// Create responsive preview based on window size
    pub fn from_window_size(window_width: f32, _window_height: f32) -> Self {
        // For mobile screens (< 800px width), use smaller preview
        let is_mobile = window_width < 800.0;

        if is_mobile {
            Self {
                show: true,
                width: (window_width * 0.35).clamp(140.0, 180.0),
                height: 80.0,
            }
        } else {
            Self::default()
        }
    }
}

impl Default for NodePreview {
    fn default() -> Self {
        Self {
            show: true,
            width: 280.0,
            height: 80.0,
        }
    }
}

/// Render NodePreview windows below each node with layer-specific content
pub fn render_node_previews(
    mut contexts: EguiContexts,
    current_layer: Res<CurrentLayer>,
    network_layer: Res<sandpolis_network::NetworkLayer>,
    mut controller_state: ResMut<NodeControllerState>,
    camera_query: Query<(&Camera, &GlobalTransform, &Projection), With<WorldView>>,
    node_query: Query<(&Transform, &NodeEntity, Option<&NodePreview>)>,
    windows: Query<&Window>,
) {
    let Ok(window) = windows.single() else {
        return;
    };

    let Ok((camera, camera_transform, projection)) = camera_query.single() else {
        return;
    };

    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };

    // Get the camera's orthographic scale (zoom level)
    let camera_scale = if let Projection::Orthographic(ortho) = projection {
        ortho.scale
    } else {
        1.0
    };

    // Get window dimensions for bounds checking
    let window_width = window.width();
    let window_height = window.height();

    // Determine if we should use minimal preview based on zoom level
    let use_minimal = camera_scale > MINIMAL_PREVIEW_ZOOM_THRESHOLD;

    // Minimal preview dimensions
    const MINIMAL_WIDTH: f32 = 150.0;
    const MINIMAL_HEIGHT: f32 = 44.0;

    // Render preview for each node
    for (transform, node_entity, preview_opt) in node_query.iter() {
        // Get preview settings
        let default_preview = NodePreview::default();
        let preview = preview_opt.unwrap_or(&default_preview);

        // Choose dimensions based on zoom level
        let (preview_width, preview_height) = if use_minimal {
            (MINIMAL_WIDTH, MINIMAL_HEIGHT)
        } else {
            (preview.width, preview.height)
        };

        // Always try to convert world position to screen position
        // This helps us determine if the node is on-screen or off-screen
        let viewport_result = camera.world_to_viewport(camera_transform, transform.translation);

        // Determine if we should show the preview
        let mut should_show = preview.show;
        let mut preview_pos = egui::Pos2::ZERO;

        if let Ok(viewport_pos) = viewport_result {
            // Calculate the node's visual radius in screen space
            let node_radius_screen = 50.0 / camera_scale;

            // Check if the node is actually visible in the viewport
            // Add some margin to account for the node's size and preview
            let margin = node_radius_screen + preview_height + 20.0;
            let is_node_visible = viewport_pos.x >= -margin
                && viewport_pos.x <= window_width + margin
                && viewport_pos.y >= -margin
                && viewport_pos.y <= window_height + margin;

            // Only show if preview is enabled AND node is visible
            should_show = should_show && is_node_visible;

            if should_show {
                // Position preview below the node with a constant distance from the bottom edge
                preview_pos = egui::Pos2::new(
                    viewport_pos.x - preview_width / 2.0,
                    viewport_pos.y + node_radius_screen + 10.0,
                );
            }
        } else {
            // world_to_viewport failed - node is definitely off-screen
            should_show = false;
        }

        // Always create the window (to allow egui to properly manage its lifecycle)
        // but use .open() to control visibility
        let mut open = should_show;

        // Use a unique ID with Hash instead of allocating String
        let window_id = egui::Id::new("preview_window").with(node_entity.instance_id);

        egui::Window::new("##preview") // Hidden title (ID makes it unique)
            .id(window_id)
            .title_bar(false)
            .resizable(false)
            .movable(false)
            .open(&mut open)
            .fixed_pos(preview_pos)
            .fixed_size([preview_width, preview_height])
            .show(ctx, |ui| {
                if should_show {
                    if use_minimal {
                        render_minimal_preview_content(
                            ui,
                            &current_layer,
                            &network_layer,
                            &mut controller_state,
                            node_entity.instance_id,
                        );
                    } else {
                        render_preview_content(
                            ui,
                            &current_layer,
                            &network_layer,
                            &mut controller_state,
                            node_entity.instance_id,
                        );
                    }
                }
            });
    }
}

/// Get layer-specific icon path for the left circular icon
pub fn get_layer_icon(layer: &Layer) -> &'static str {
    match layer.name() {
        "Filesystem" => "📁",
        "Shell" => "💻",
        "Inventory" => "📊",
        "Desktop" => "🖥",
        "Network" => "🌐",
        _ => "📦",
    }
}

/// Get hostname for an instance
pub fn get_hostname(instance_id: InstanceId) -> String {
    queries::query_instance_metadata(instance_id)
        .ok()
        .and_then(|m| m.hostname)
        .unwrap_or_else(|| format!("Node {}", instance_id))
}

/// Get layer-specific bottom line details
pub fn get_layer_details(layer: &Layer, network_layer: &sandpolis_network::NetworkLayer, instance_id: InstanceId) -> String {
    match layer.name() {
        #[cfg(feature = "layer-filesystem")]
        "Filesystem" => {
            if let Ok(usage) = queries::query_filesystem_usage(instance_id) {
                let used_gb = usage.used as f64 / 1_000_000_000.0;
                let total_gb = usage.total as f64 / 1_000_000_000.0;
                if total_gb > 0.0 {
                    let percent = (usage.used as f64 / usage.total as f64) * 100.0;
                    format!("{:.1} GB / {:.1} GB ({:.0}%)", used_gb, total_gb, percent)
                } else {
                    "No filesystem data".to_string()
                }
            } else {
                "No filesystem data".to_string()
            }
        }

        "Network" => {
            if let Ok(stats) = queries::query_network_stats(network_layer, instance_id) {
                if let Some(latency) = stats.latency_ms {
                    format!("Latency: {} ms", latency)
                } else {
                    "No network connection".to_string()
                }
            } else {
                "No network connection".to_string()
            }
        }

        #[cfg(feature = "layer-inventory")]
        "Inventory" => {
            if let Ok(mem) = queries::query_memory_stats(instance_id) {
                let percent = if mem.total > 0 {
                    (mem.used as f64 / mem.total as f64) * 100.0
                } else {
                    0.0
                };
                format!("Memory: {:.0}% used", percent)
            } else {
                "No system data".to_string()
            }
        }

        #[cfg(feature = "layer-shell")]
        "Shell" => {
            if let Ok(sessions) = queries::query_shell_sessions(instance_id) {
                let active_count = sessions.iter().filter(|s| s.active).count();
                format!("{} session{}, {} active",
                    sessions.len(),
                    if sessions.len() == 1 { "" } else { "s" },
                    active_count
                )
            } else {
                "No shell sessions".to_string()
            }
        }

        #[cfg(feature = "layer-desktop")]
        "Desktop" => {
            if let Ok(metadata) = queries::query_instance_metadata(instance_id) {
                format!("OS: {:?}", metadata.os_type)
            } else {
                "No desktop data".to_string()
            }
        }

        _ => format!("Layer: {}", layer),
    }
}

/// Render the new 4-part preview layout
pub fn render_preview_content(
    ui: &mut egui::Ui,
    current_layer: &Layer,
    network_layer: &sandpolis_network::NetworkLayer,
    controller_state: &mut NodeControllerState,
    instance_id: InstanceId,
) {
    // Get data for the preview
    let hostname = get_hostname(instance_id);
    let layer_icon = get_layer_icon(current_layer);
    let layer_details = get_layer_details(current_layer, network_layer, instance_id);

    // Push unique ID scope to prevent collisions between multiple preview windows
    ui.push_id(instance_id.to_string(), |ui| {
        // Use a horizontal layout for the main content
        ui.horizontal(|ui| {
        // Left side: Circular icon area (60x60 pixels)
        ui.vertical(|ui| {
            ui.set_width(60.0);
            ui.set_height(60.0);

            // Create circular background
            let (rect, _response) = ui.allocate_exact_size(
                egui::vec2(60.0, 60.0),
                egui::Sense::hover()
            );

            // Draw circle background
            let center = rect.center();
            let radius = 28.0;
            ui.painter().circle_filled(
                center,
                radius,
                ui.visuals().widgets.inactive.bg_fill,
            );

            // Draw icon text centered in circle
            ui.painter().text(
                center,
                egui::Align2::CENTER_CENTER,
                layer_icon,
                egui::FontId::proportional(32.0),
                ui.visuals().text_color(),
            );
        });

        ui.add_space(8.0);

        // Center: Hostname (top) and Details (bottom)
        ui.vertical(|ui| {
            ui.set_height(60.0);
            ui.set_width(160.0);

            // Top line: Hostname
            ui.label(
                egui::RichText::new(&hostname)
                    .strong()
                    .size(14.0)
            );

            ui.add_space(4.0);

            // Bottom line: Layer-specific details
            ui.label(
                egui::RichText::new(&layer_details)
                    .size(11.0)
                    .color(ui.visuals().weak_text_color())
            );
        });

        ui.add_space(4.0);

        // Right side: Button to open controller
        ui.vertical(|ui| {
            ui.set_width(40.0);
            ui.set_height(60.0);

            // Center the button vertically
            ui.add_space(10.0);

            // Create circular button
            let button_size = egui::vec2(40.0, 40.0);
            let (rect, response) = ui.allocate_exact_size(button_size, egui::Sense::click());

            // Change color on hover/click
            let bg_color = if response.clicked() {
                ui.visuals().widgets.active.bg_fill
            } else if response.hovered() {
                ui.visuals().widgets.hovered.bg_fill
            } else {
                ui.visuals().widgets.inactive.bg_fill
            };

            // Draw circular button background
            let center = rect.center();
            let radius = 18.0;
            ui.painter().circle_filled(center, radius, bg_color);

            // Draw arrow icon in button
            ui.painter().text(
                center,
                egui::Align2::CENTER_CENTER,
                "→",
                egui::FontId::proportional(24.0),
                ui.visuals().text_color(),
            );

            // Handle click to open controller
            if response.clicked() {
                controller_state.expanded_node = Some(instance_id);
                controller_state.controller_type = ControllerType::from_layer(current_layer);
                info!("Opening {:?} controller for {}", controller_state.controller_type, instance_id);
            }

            // Show tooltip on hover
            if response.hovered() {
                response.on_hover_text(format!("Open {} controller", ControllerType::from_layer(current_layer).display_name()));
            }
        });
        });
    });
}

/// Render a minimal preview showing just the layer icon and hostname
/// Used when zoomed out to reduce visual clutter
fn render_minimal_preview_content(
    ui: &mut egui::Ui,
    current_layer: &Layer,
    network_layer: &sandpolis_network::NetworkLayer,
    controller_state: &mut NodeControllerState,
    instance_id: InstanceId,
) {
    let _ = network_layer; // May be used in future for network stats
    let hostname = get_hostname(instance_id);
    let layer_icon = get_layer_icon(current_layer);

    // Push unique ID scope to prevent collisions between multiple preview windows
    ui.push_id(instance_id.to_string(), |ui| {
        ui.horizontal(|ui| {
            // Small circular icon (36x36)
            ui.vertical(|ui| {
                ui.set_width(36.0);
                ui.set_height(36.0);

                let (rect, _response) = ui.allocate_exact_size(
                    egui::vec2(36.0, 36.0),
                    egui::Sense::hover()
                );

                // Draw circle background
                let center = rect.center();
                let radius = 16.0;
                ui.painter().circle_filled(
                    center,
                    radius,
                    ui.visuals().widgets.inactive.bg_fill,
                );

                // Draw icon
                ui.painter().text(
                    center,
                    egui::Align2::CENTER_CENTER,
                    layer_icon,
                    egui::FontId::proportional(18.0),
                    ui.visuals().text_color(),
                );
            });

            ui.add_space(4.0);

            // Hostname only (truncated if needed)
            ui.vertical(|ui| {
                ui.set_height(36.0);
                ui.set_width(80.0);

                ui.add_space(8.0);

                // Truncate hostname if too long
                let display_name = if hostname.len() > 12 {
                    format!("{}...", &hostname[..9])
                } else {
                    hostname.clone()
                };

                ui.label(
                    egui::RichText::new(&display_name)
                        .strong()
                        .size(11.0)
                );
            });

            // Small clickable arrow button
            ui.vertical(|ui| {
                ui.set_width(24.0);
                ui.set_height(36.0);

                ui.add_space(6.0);

                let button_size = egui::vec2(24.0, 24.0);
                let (rect, response) = ui.allocate_exact_size(button_size, egui::Sense::click());

                let bg_color = if response.clicked() {
                    ui.visuals().widgets.active.bg_fill
                } else if response.hovered() {
                    ui.visuals().widgets.hovered.bg_fill
                } else {
                    ui.visuals().widgets.inactive.bg_fill
                };

                let center = rect.center();
                let radius = 10.0;
                ui.painter().circle_filled(center, radius, bg_color);

                ui.painter().text(
                    center,
                    egui::Align2::CENTER_CENTER,
                    "→",
                    egui::FontId::proportional(14.0),
                    ui.visuals().text_color(),
                );

                if response.clicked() {
                    controller_state.expanded_node = Some(instance_id);
                    controller_state.controller_type = ControllerType::from_layer(current_layer);
                }

                if response.hovered() {
                    response.on_hover_text(format!("Open {} controller", ControllerType::from_layer(current_layer).display_name()));
                }
            });
        });
    });
}

/// Toggle NodePreview visibility based on user input
pub fn toggle_node_preview_visibility(
    mut contexts: bevy_egui::EguiContexts,
    keyboard: Res<ButtonInput<KeyCode>>,
    mut node_query: Query<&mut NodePreview>,
) {
    // Don't handle hotkey if egui wants keyboard input
    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };
    if ctx.wants_keyboard_input() {
        return;
    }

    // Press 'P' to toggle preview visibility
    if keyboard.just_pressed(KeyCode::KeyP) {
        for mut preview in node_query.iter_mut() {
            preview.show = !preview.show;
        }
    }
}
