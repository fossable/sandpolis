use super::{components::NodeEntity, WorldView};
use bevy::prelude::*;
use bevy_egui::{egui, EguiContexts};
use sandpolis_core::InstanceId;

/// Resource to track node picker UI state
#[derive(Resource)]
pub struct NodePickerState {
    pub show: bool,
    pub search_query: String,
    pub selected_index: usize,
}

impl Default for NodePickerState {
    fn default() -> Self {
        Self {
            show: false,
            search_query: String::new(),
            selected_index: 0,
        }
    }
}

/// Cached node information for display
#[derive(Clone)]
struct NodeInfo {
    pub instance_id: InstanceId,
    pub is_server: bool,
    pub entity: Entity,
}

/// Get emoji for node type
fn get_node_emoji(is_server: bool) -> &'static str {
    if is_server {
        "🖧"
    } else {
        "🤖"
    }
}

/// Render node picker panel
pub fn render_node_picker_panel(
    mut contexts: EguiContexts,
    mut picker_state: ResMut<NodePickerState>,
    nodes: Query<(Entity, &InstanceId)>,
    mut camera_query: Query<&mut Transform, With<WorldView>>,
    node_transforms: Query<&Transform, (With<InstanceId>, Without<WorldView>)>,
    windows: Query<&Window>,
    keyboard: Res<ButtonInput<KeyCode>>,
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

    // Collect all nodes
    let mut all_nodes: Vec<NodeInfo> = nodes
        .iter()
        .map(|(entity, instance_id)| NodeInfo {
            instance_id: *instance_id,
            is_server: instance_id.is_server(),
            entity,
        })
        .collect();

    // Sort by instance ID for consistent ordering
    all_nodes.sort_by_key(|n| n.instance_id);

    let search_query = picker_state.search_query.to_lowercase();

    // Filter nodes based on search query
    let filtered_nodes: Vec<_> = all_nodes
        .iter()
        .filter(|node| {
            if search_query.is_empty() {
                true
            } else {
                // Search by instance ID or server/agent status
                format!("{}", node.instance_id).to_lowercase().contains(&search_query)
                    || (node.is_server && "server".contains(&search_query))
                    || (!node.is_server && "agent".contains(&search_query))
            }
        })
        .collect();

    // Clamp selected index
    if picker_state.selected_index >= filtered_nodes.len() && !filtered_nodes.is_empty() {
        picker_state.selected_index = filtered_nodes.len() - 1;
    }

    // Panel dimensions
    let panel_width = if is_mobile {
        window_size.x * 0.9
    } else {
        400.0
    };
    let panel_height = if is_mobile {
        window_size.y * 0.7
    } else {
        500.0
    };

    // Center the panel
    let panel_pos = egui::Pos2::new(
        (window_size.x - panel_width) / 2.0,
        (window_size.y - panel_height) / 2.0,
    );

    let mut should_close = false;
    let mut target_node: Option<(Entity, InstanceId)> = None;

    egui::Window::new("Find Node")
        .fixed_pos(panel_pos)
        .fixed_size([panel_width, panel_height])
        .collapsible(false)
        .resizable(false)
        .show(ctx, |ui| {
            ui.heading("🔍 Search Nodes");
            ui.separator();

            // Search input
            ui.horizontal(|ui| {
                ui.label("Search:");
                let text_edit = egui::TextEdit::singleline(&mut picker_state.search_query)
                    .hint_text("ID, OS, hostname, server/agent...")
                    .desired_width(panel_width - 100.0);

                let response = ui.add(text_edit);

                // Auto-focus the search box when the picker opens
                if picker_state.is_changed() && picker_state.show {
                    response.request_focus();
                }

                // Handle Enter key to select highlighted node
                if response.lost_focus() && ui.input(|i| i.key_pressed(egui::Key::Enter)) {
                    if !filtered_nodes.is_empty() {
                        let selected_node = &filtered_nodes[picker_state.selected_index];
                        target_node = Some((selected_node.entity, selected_node.instance_id));
                        should_close = true;
                    }
                }
            });

            ui.add_space(8.0);

            // Results count
            ui.label(format!(
                "Found {} node{}",
                filtered_nodes.len(),
                if filtered_nodes.len() == 1 { "" } else { "s" }
            ));

            ui.add_space(4.0);

            // Node list
            egui::ScrollArea::vertical().show(ui, |ui| {
                let button_height = if is_mobile { 60.0 } else { 50.0 };

                if filtered_nodes.is_empty() {
                    ui.label("No matching nodes found");
                } else {
                    for (index, node) in filtered_nodes.iter().enumerate() {
                        let is_selected = index == picker_state.selected_index;

                        // Build display text
                        let display_text = format!(
                            "{} {} ({})",
                            get_node_emoji(node.is_server),
                            node.instance_id,
                            if node.is_server { "Server" } else { "Agent" }
                        );

                        let button_text = egui::RichText::new(display_text)
                            .size(if is_mobile { 16.0 } else { 14.0 });

                        let button = egui::Button::new(button_text)
                            .fill(if is_selected {
                                egui::Color32::from_rgb(60, 120, 180)
                            } else {
                                egui::Color32::from_gray(40)
                            })
                            .min_size(egui::vec2(panel_width - 40.0, button_height));

                        if ui.add(button).clicked() {
                            target_node = Some((node.entity, node.instance_id));
                            should_close = true;
                        }

                        ui.add_space(if is_mobile { 6.0 } else { 4.0 });
                    }
                }
            });

            ui.separator();

            // Close button and hints at bottom
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
                    should_close = true;
                }

                ui.add_space(4.0);
                ui.label(
                    egui::RichText::new("Press Enter to jump to selected node")
                        .size(12.0)
                        .color(egui::Color32::from_gray(150)),
                );
            });
        });

    // Center camera on selected node
    if let Some((target_entity, target_id)) = target_node {
        if let (Ok(mut camera_transform), Ok(node_transform)) = (
            camera_query.single_mut(),
            node_transforms.get(target_entity),
        ) {
            // Move camera to center on the node (preserve Z coordinate)
            camera_transform.translation.x = node_transform.translation.x;
            camera_transform.translation.y = node_transform.translation.y;

            info!("Centered camera on node: {}", target_id);
        }
    }

    if should_close {
        picker_state.show = false;
        picker_state.search_query.clear();
        picker_state.selected_index = 0;
    }

    // Handle Escape key to close the picker
    if !ctx.wants_keyboard_input() && keyboard.just_pressed(KeyCode::Escape) {
        picker_state.show = false;
        picker_state.search_query.clear();
        picker_state.selected_index = 0;
    }

    // Handle arrow keys for navigation (when not typing)
    if !ctx.wants_keyboard_input() && !filtered_nodes.is_empty() {
        if keyboard.just_pressed(KeyCode::ArrowDown) {
            picker_state.selected_index = (picker_state.selected_index + 1).min(filtered_nodes.len() - 1);
        }
        if keyboard.just_pressed(KeyCode::ArrowUp) {
            picker_state.selected_index = picker_state.selected_index.saturating_sub(1);
        }
    }
}

/// Handle keyboard shortcut to toggle node picker
pub fn handle_node_picker_toggle(
    mut contexts: EguiContexts,
    keyboard: Res<ButtonInput<KeyCode>>,
    mut picker_state: ResMut<NodePickerState>,
) {
    // Don't handle if egui wants keyboard input
    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };
    if ctx.wants_keyboard_input() {
        return;
    }

    // Toggle node picker with N key
    if keyboard.just_pressed(KeyCode::KeyN) {
        picker_state.show = !picker_state.show;
        // Clear search query when opening
        if picker_state.show {
            picker_state.search_query.clear();
            picker_state.selected_index = 0;
        }
    }
}
