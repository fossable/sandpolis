use super::components::{NodeEntity, WorldView};
use bevy::prelude::*;
use bevy::window::PrimaryWindow;
use bevy_rapier2d::prelude::*;

/// Tracks the current drag operation
#[derive(Resource, Default)]
pub struct DragState {
    pub dragging_entity: Option<Entity>,
    pub drag_offset: Vec2,
}

/// Marker component for nodes that are currently being dragged
#[derive(Component)]
pub struct Dragging;

/// Detect mouse click on nodes and start dragging
pub fn start_node_drag(
    mouse_button: Res<ButtonInput<MouseButton>>,
    windows: Query<&Window, With<PrimaryWindow>>,
    camera_query: Query<(&Camera, &GlobalTransform), With<WorldView>>,
    mut drag_state: ResMut<DragState>,
    mut commands: Commands,
    node_query: Query<(Entity, &Transform), With<NodeEntity>>,
) {
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
