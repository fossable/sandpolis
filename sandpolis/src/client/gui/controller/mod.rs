use super::{CurrentLayer, components::NodeEntity};
use sandpolis_core::Layer;
use bevy::prelude::*;
use bevy_egui::{EguiContexts, egui};
use sandpolis_core::InstanceId;

pub mod file_browser;
pub mod terminal;
pub mod system_info;
pub mod package_manager;
pub mod desktop_viewer;

/// Resource tracking which node controller is currently open
#[derive(Resource, Default)]
pub struct NodeControllerState {
    pub expanded_node: Option<InstanceId>,
    pub controller_type: ControllerType,
    pub window_size: Vec2,
}

impl NodeControllerState {
    /// Get responsive controller dimensions based on window size
    pub fn get_controller_dimensions(window_width: f32, window_height: f32) -> (f32, f32) {
        // For mobile screens (< 800px width), use full screen with padding
        let is_mobile = window_width < 800.0;

        if is_mobile {
            // Use 95% of screen with minimum padding
            let width = (window_width * 0.95).max(280.0);
            let height = (window_height * 0.80).max(400.0);
            (width, height)
        } else {
            // Desktop: fixed size
            (600.0, 400.0)
        }
    }
}

/// Types of controllers available
#[derive(Debug, Default, Clone, Copy, PartialEq, Eq)]
pub enum ControllerType {
    #[default]
    None,
    FileBrowser,
    Terminal,
    SystemInfo,
    PackageManager,
    DesktopViewer,
}

impl ControllerType {
    /// Get the controller type for a given layer
    pub fn from_layer(layer: &Layer) -> Self {
        match layer.name() {
            #[cfg(feature = "layer-filesystem")]
            "Filesystem" => ControllerType::FileBrowser,

            #[cfg(feature = "layer-shell")]
            "Shell" => ControllerType::Terminal,

            #[cfg(feature = "layer-inventory")]
            "Inventory" => ControllerType::SystemInfo,

            #[cfg(feature = "layer-desktop")]
            "Desktop" => ControllerType::DesktopViewer,

            _ => ControllerType::SystemInfo, // Default fallback
        }
    }

    /// Get display name for this controller
    pub fn display_name(&self) -> &'static str {
        match self {
            ControllerType::None => "None",
            ControllerType::FileBrowser => "File Browser",
            ControllerType::Terminal => "Terminal",
            ControllerType::SystemInfo => "System Information",
            ControllerType::PackageManager => "Package Manager",
            ControllerType::DesktopViewer => "Desktop Viewer",
        }
    }
}

/// Detect double-click on nodes to open controller
pub fn handle_node_double_click(
    mut contexts: EguiContexts,
    mouse_button: Res<ButtonInput<MouseButton>>,
    time: Res<Time>,
    windows: Query<&Window>,
    camera_query: Query<(&Camera, &GlobalTransform), With<super::components::WorldView>>,
    node_query: Query<(&Transform, &NodeEntity)>,
    current_layer: Res<CurrentLayer>,
    mut controller_state: ResMut<NodeControllerState>,
    mut last_click: Local<(f32, Option<InstanceId>)>, // (time, entity)
) {
    // Don't handle clicks if egui wants the input
    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };
    if ctx.wants_pointer_input() || ctx.is_pointer_over_area() {
        return;
    }

    if !mouse_button.just_pressed(MouseButton::Left) {
        return;
    }

    let Ok(window) = windows.single() else {
        return;
    };

    let Some(cursor_position) = window.cursor_position() else {
        return;
    };

    let Ok((camera, camera_transform)) = camera_query.single() else {
        return;
    };

    let Ok(world_position) = camera.viewport_to_world_2d(camera_transform, cursor_position) else {
        return;
    };

    // Find clicked node
    const CLICK_RADIUS: f32 = 50.0;
    let mut clicked_node: Option<InstanceId> = None;

    for (transform, node_entity) in node_query.iter() {
        let node_pos = transform.translation.truncate();
        let distance = world_position.distance(node_pos);

        if distance <= CLICK_RADIUS {
            clicked_node = Some(node_entity.instance_id);
            break;
        }
    }

    // Check for double-click
    let current_time = time.elapsed_secs();
    let (last_time, last_entity) = *last_click;

    if let Some(clicked_id) = clicked_node {
        if current_time - last_time < 0.3 && last_entity == Some(clicked_id) {
            // Double-click detected - open/close controller
            if controller_state.expanded_node == Some(clicked_id) {
                // Close if already open
                controller_state.expanded_node = None;
                controller_state.controller_type = ControllerType::None;
            } else {
                // Open controller for this node
                controller_state.expanded_node = Some(clicked_id);
                controller_state.controller_type = ControllerType::from_layer(&current_layer);
            }
        }
        *last_click = (current_time, Some(clicked_id));
    }
}

/// Render the node controller window
pub fn render_node_controller(
    mut contexts: EguiContexts,
    mut controller_state: ResMut<NodeControllerState>,
    network_layer: Res<sandpolis_network::NetworkLayer>,
    windows: Query<&Window>,
) {
    let Some(instance_id) = controller_state.expanded_node else {
        return;
    };

    if controller_state.controller_type == ControllerType::None {
        return;
    }

    let Ok(window) = windows.single() else {
        return;
    };

    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };

    let window_size = Vec2::new(window.width(), window.height());

    // Get responsive controller dimensions
    let (controller_width, controller_height) =
        NodeControllerState::get_controller_dimensions(window_size.x, window_size.y);

    // Center the controller window
    let controller_pos = egui::Pos2::new(
        (window_size.x - controller_width) / 2.0,
        (window_size.y - controller_height) / 2.0,
    );

    controller_state.window_size = Vec2::new(controller_width, controller_height);

    egui::Window::new(controller_state.controller_type.display_name())
        .fixed_pos(controller_pos)
        .fixed_size([controller_width, controller_height])
        .collapsible(false)
        .resizable(false)
        .show(ctx, |ui| {
            // Close button
            ui.horizontal(|ui| {
                ui.label(format!("Instance: {}", instance_id));
                ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                    if ui.button("Close").clicked() {
                        controller_state.expanded_node = None;
                        controller_state.controller_type = ControllerType::None;
                    }
                });
            });

            ui.separator();

            // Render controller-specific content
            match controller_state.controller_type {
                ControllerType::FileBrowser => {
                    #[cfg(feature = "layer-filesystem")]
                    file_browser::render(ui, instance_id);
                    #[cfg(not(feature = "layer-filesystem"))]
                    ui.label("Filesystem layer not enabled");
                }
                ControllerType::Terminal => {
                    #[cfg(feature = "layer-shell")]
                    terminal::render(ui, instance_id);
                    #[cfg(not(feature = "layer-shell"))]
                    ui.label("Shell layer not enabled");
                }
                ControllerType::SystemInfo => {
                    #[cfg(feature = "layer-inventory")]
                    system_info::render(ui, &network_layer, instance_id);
                    #[cfg(not(feature = "layer-inventory"))]
                    ui.label("Inventory layer not enabled");
                }
                ControllerType::PackageManager => {
                    package_manager::render(ui, instance_id);
                }
                ControllerType::DesktopViewer => {
                    #[cfg(feature = "layer-desktop")]
                    desktop_viewer::render(ui, instance_id);
                    #[cfg(not(feature = "layer-desktop"))]
                    ui.label("Desktop layer not enabled");
                }
                ControllerType::None => {}
            }
        });
}

/// Close controller when switching layers
pub fn close_controller_on_layer_change(
    current_layer: Res<CurrentLayer>,
    mut controller_state: ResMut<NodeControllerState>,
) {
    if current_layer.is_changed() && !current_layer.is_added() {
        controller_state.expanded_node = None;
        controller_state.controller_type = ControllerType::None;
    }
}
