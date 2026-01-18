use crate::gui::node::{NodeEntity, Selected, WorldView};
use bevy::prelude::*;
use bevy::window::PrimaryWindow;
use bevy_egui::{EguiContexts, egui};
use bevy_rapier2d::prelude::*;
use sandpolis_core::InstanceId;

/// Resource tracking all currently selected nodes.
#[derive(Resource, Default)]
pub struct SelectionSet {
    pub selected_nodes: Vec<InstanceId>,
}

/// Tracks the current drag operation
#[derive(Resource, Default)]
pub struct DragState {
    pub dragging_entity: Option<Entity>,
    pub drag_offset: Vec2,
}

/// Marker component for nodes that are currently being dragged
#[derive(Component)]
pub struct Dragging;

/// Handle node selection on click (single-click to select, Ctrl-click to multi-select)
pub fn handle_node_selection(
    mut contexts: EguiContexts,
    mouse_button: Res<ButtonInput<MouseButton>>,
    keyboard: Res<ButtonInput<KeyCode>>,
    windows: Query<&Window, With<PrimaryWindow>>,
    camera_query: Query<(&Camera, &GlobalTransform), With<WorldView>>,
    mut commands: Commands,
    node_query: Query<(Entity, &Transform, &NodeEntity)>,
    mut selection_set: ResMut<SelectionSet>,
) {
    // Don't handle selection if egui wants the input
    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };
    if ctx.wants_pointer_input() || ctx.is_pointer_over_area() {
        return;
    }

    // Only handle on left mouse button press
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

    // Convert screen coordinates to world coordinates
    let Ok(world_position) = camera.viewport_to_world_2d(camera_transform, cursor_position) else {
        return;
    };

    // Check which node was clicked
    const CLICK_RADIUS: f32 = 50.0;
    let mut clicked_node: Option<(Entity, sandpolis_core::InstanceId)> = None;

    for (entity, transform, node_entity) in node_query.iter() {
        let node_pos = transform.translation.truncate();
        let distance = world_position.distance(node_pos);

        if distance <= CLICK_RADIUS {
            clicked_node = Some((entity, node_entity.instance_id));
            break;
        }
    }

    // Check if Ctrl/Command is pressed for multi-selection
    let ctrl_pressed = keyboard.pressed(KeyCode::ControlLeft)
        || keyboard.pressed(KeyCode::ControlRight)
        || keyboard.pressed(KeyCode::SuperLeft)  // Command on Mac
        || keyboard.pressed(KeyCode::SuperRight);

    if let Some((entity, instance_id)) = clicked_node {
        if ctrl_pressed {
            // Multi-select mode: toggle selection
            if selection_set.selected_nodes.contains(&instance_id) {
                // Deselect
                selection_set.selected_nodes.retain(|&id| id != instance_id);
                commands.entity(entity).remove::<Selected>();
            } else {
                // Add to selection
                selection_set.selected_nodes.push(instance_id);
                commands.entity(entity).insert(Selected);
            }
        } else {
            // Single-select mode: clear all selections and select only this node
            // First, clear all existing selections
            for (entity, _, node_entity) in node_query.iter() {
                if selection_set
                    .selected_nodes
                    .contains(&node_entity.instance_id)
                {
                    commands.entity(entity).remove::<Selected>();
                }
            }

            // Now select only the clicked node
            selection_set.selected_nodes.clear();
            selection_set.selected_nodes.push(instance_id);
            commands.entity(entity).insert(Selected);
        }
    } else if !ctrl_pressed {
        // Clicked empty space without Ctrl: clear all selections
        for (entity, _, node_entity) in node_query.iter() {
            if selection_set
                .selected_nodes
                .contains(&node_entity.instance_id)
            {
                commands.entity(entity).remove::<Selected>();
            }
        }
        selection_set.selected_nodes.clear();
    }
}

/// Detect mouse click on nodes and start dragging
pub fn start_node_drag(
    mut contexts: EguiContexts,
    mouse_button: Res<ButtonInput<MouseButton>>,
    windows: Query<&Window, With<PrimaryWindow>>,
    camera_query: Query<(&Camera, &GlobalTransform), With<WorldView>>,
    mut drag_state: ResMut<DragState>,
    mut commands: Commands,
    node_query: Query<(Entity, &Transform), With<NodeEntity>>,
) {
    // Don't start drag if egui wants the input
    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };
    if ctx.wants_pointer_input() || ctx.is_pointer_over_area() {
        return;
    }

    // Only start drag on left mouse button press
    if !mouse_button.just_pressed(MouseButton::Left) {
        return;
    };

    let Ok(window) = windows.single() else {
        return;
    };

    let Some(cursor_position) = window.cursor_position() else {
        return;
    };

    let Ok((camera, camera_transform)) = camera_query.single() else {
        return;
    };

    // Convert screen coordinates to world coordinates
    let Ok(world_position) = camera.viewport_to_world_2d(camera_transform, cursor_position) else {
        return;
    };

    // Check which node was clicked (simple distance-based check)
    const CLICK_RADIUS: f32 = 50.0; // Match the collider radius

    for (entity, transform) in node_query.iter() {
        let node_pos = transform.translation.truncate();
        let distance = world_position.distance(node_pos);

        if distance <= CLICK_RADIUS {
            drag_state.dragging_entity = Some(entity);
            drag_state.drag_offset = world_position - node_pos;
            commands.entity(entity).insert(Dragging);
            break;
        }
    }
}

/// Update the position of the dragged node to follow the mouse
pub fn update_node_drag(
    windows: Query<&Window, With<PrimaryWindow>>,
    camera_query: Query<(&Camera, &GlobalTransform), With<WorldView>>,
    drag_state: Res<DragState>,
    mut nodes: Query<(&mut Transform, &mut Velocity), With<Dragging>>,
) {
    let Some(dragging_entity) = drag_state.dragging_entity else {
        return;
    };

    let Ok(window) = windows.single() else {
        return;
    };

    let Some(cursor_position) = window.cursor_position() else {
        return;
    };

    let Ok((camera, camera_transform)) = camera_query.single() else {
        return;
    };

    // Convert screen coordinates to world coordinates
    let Ok(world_position) = camera.viewport_to_world_2d(camera_transform, cursor_position) else {
        return;
    };

    // Update the dragged node's position
    if let Ok((mut transform, mut velocity)) = nodes.get_mut(dragging_entity) {
        let target_position = world_position - drag_state.drag_offset;
        transform.translation.x = target_position.x;
        transform.translation.y = target_position.y;

        // Zero out velocity while dragging to prevent physics interference
        velocity.linvel = Vec2::ZERO;
        velocity.angvel = 0.0;
    }
}

/// Stop dragging when mouse button is released
pub fn stop_node_drag(
    mouse_button: Res<ButtonInput<MouseButton>>,
    mut drag_state: ResMut<DragState>,
    mut commands: Commands,
) {
    if mouse_button.just_released(MouseButton::Left) {
        if let Some(entity) = drag_state.dragging_entity {
            // Remove Dragging marker component
            commands.entity(entity).remove::<Dragging>();
        }

        drag_state.dragging_entity = None;
        drag_state.drag_offset = Vec2::ZERO;
    }
}

/// Disable physics forces on dragged nodes
pub fn disable_forces_while_dragging(mut nodes: Query<&mut ExternalForce, With<Dragging>>) {
    for mut force in nodes.iter_mut() {
        force.force = Vec2::ZERO;
        force.torque = 0.0;
    }
}

/// Visual component for selection ring
#[derive(Component)]
pub struct SelectionRing {
    pub node_entity: Entity,
}

/// Spawn/update selection rings for selected nodes
pub fn update_selection_visuals(
    mut commands: Commands,
    selected_nodes: Query<Entity, (With<NodeEntity>, With<Selected>)>,
    selection_rings: Query<(Entity, &SelectionRing)>,
    mut meshes: ResMut<Assets<Mesh>>,
    mut materials: ResMut<Assets<ColorMaterial>>,
) {
    // Remove rings for nodes that are no longer selected
    for (ring_entity, selection_ring) in selection_rings.iter() {
        if !selected_nodes.contains(selection_ring.node_entity) {
            commands.entity(ring_entity).despawn();
        }
    }

    // Add rings for newly selected nodes
    for node_entity in selected_nodes.iter() {
        // Check if this node already has a selection ring
        let has_ring = selection_rings
            .iter()
            .any(|(_, ring)| ring.node_entity == node_entity);

        if !has_ring {
            // Create a ring mesh (hollow circle)
            let ring = Mesh::from(Circle::new(55.0));

            // Spawn selection ring as a child of the node
            commands.entity(node_entity).with_children(|parent| {
                parent.spawn((
                    Mesh2d(meshes.add(ring)),
                    MeshMaterial2d(
                        materials.add(ColorMaterial::from(Color::srgba(0.3, 0.8, 1.0, 0.6))),
                    ),
                    Transform::from_xyz(0.0, 0.0, -0.1), // Behind the node
                    SelectionRing { node_entity },
                ));
            });
        }
    }
}

/// Render UI element showing selected node count
pub fn render_selection_ui(
    mut contexts: EguiContexts,
    selection_set: Res<SelectionSet>,
    windows: Query<&Window>,
) {
    // Only show UI when multiple nodes are selected
    if selection_set.selected_nodes.len() <= 1 {
        return;
    }

    let Ok(ctx) = contexts.ctx_mut() else {
        return;
    };

    let Ok(window) = windows.single() else {
        return;
    };

    let window_size = bevy::math::Vec2::new(window.width(), window.height());

    // Show selection count in top-right corner
    egui::Window::new("Selection")
        .id(egui::Id::new("selection_count"))
        .title_bar(false)
        .resizable(false)
        .movable(false)
        .fixed_pos(egui::Pos2::new(window_size.x - 120.0, 10.0))
        .show(ctx, |ui| {
            ui.horizontal(|ui| {
                ui.label(format!(
                    "📌 {} nodes selected",
                    selection_set.selected_nodes.len()
                ));
            });
        });
}
