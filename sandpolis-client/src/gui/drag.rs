use crate::gui::node::{ExcludeFromSelection, NodeHitbox, Selected, WorldView};
use crate::gui::ui::gating::UiPointerState;
use bevy::prelude::*;
use bevy::window::PrimaryWindow;
use bevy_rapier2d::prelude::*;

/// Resource tracking all currently selected nodes.
///
/// Entity-keyed rather than `InstanceId`-keyed, because not every selectable
/// node is an instance: account nodes have an `AccountId` and no `InstanceId`.
/// Look up `NodeEntity` on an entity when the instance is what you need.
#[derive(Resource, Default)]
pub struct SelectionSet {
    pub selected_nodes: Vec<Entity>,
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

/// The visible node whose hitbox the cursor is inside, nearest one first.
///
/// Hidden nodes are skipped: a layer that filters out an instance type leaves
/// those nodes in the world, and clicking where one used to be shouldn't select
/// or drag it.
pub fn node_at(
    world_position: Vec2,
    nodes: impl Iterator<Item = (Entity, Vec2, f32, bool)>,
) -> Option<Entity> {
    nodes
        .filter(|(_, _, _, visible)| *visible)
        .filter_map(|(entity, position, radius, _)| {
            let distance = world_position.distance(position);
            (distance <= radius).then_some((entity, distance))
        })
        .min_by(|(_, a), (_, b)| a.total_cmp(b))
        .map(|(entity, _)| entity)
}

/// Whether a node with this visibility should respond to the pointer.
pub fn is_visible(visibility: Option<&Visibility>) -> bool {
    visibility != Some(&Visibility::Hidden)
}

/// Where the cursor is in world space, if it's over the window.
pub fn cursor_world_position(
    windows: &Query<&Window, With<PrimaryWindow>>,
    cameras: &Query<(&Camera, &GlobalTransform), With<WorldView>>,
) -> Option<Vec2> {
    let cursor = windows.single().ok()?.cursor_position()?;
    let (camera, camera_transform) = cameras.single().ok()?;
    camera.viewport_to_world_2d(camera_transform, cursor).ok()
}

/// Handle node selection on click (single-click to select, Ctrl-click to multi-select)
pub fn handle_node_selection(
    ui_pointer: Res<UiPointerState>,
    mouse_button: Res<ButtonInput<MouseButton>>,
    keyboard: Res<ButtonInput<KeyCode>>,
    windows: Query<&Window, With<PrimaryWindow>>,
    camera_query: Query<(&Camera, &GlobalTransform), With<WorldView>>,
    mut commands: Commands,
    node_query: Query<
        (Entity, &Transform, &NodeHitbox, Option<&Visibility>),
        Without<ExcludeFromSelection>,
    >,
    mut selection_set: ResMut<SelectionSet>,
) {
    // Don't handle selection if the pointer is over blocking UI
    if ui_pointer.over_ui_blocking {
        return;
    }

    // Only handle on left mouse button press
    if !mouse_button.just_pressed(MouseButton::Left) {
        return;
    }

    let Some(world_position) = cursor_world_position(&windows, &camera_query) else {
        return;
    };

    let clicked_node = node_at(
        world_position,
        node_query.iter().map(|(entity, transform, hitbox, vis)| {
            (
                entity,
                transform.translation.truncate(),
                hitbox.radius,
                is_visible(vis),
            )
        }),
    );

    // Check if Ctrl/Command is pressed for multi-selection
    let ctrl_pressed = keyboard.pressed(KeyCode::ControlLeft)
        || keyboard.pressed(KeyCode::ControlRight)
        || keyboard.pressed(KeyCode::SuperLeft)  // Command on Mac
        || keyboard.pressed(KeyCode::SuperRight);

    let clear_all = |commands: &mut Commands, selection_set: &mut SelectionSet| {
        for entity in selection_set.selected_nodes.drain(..) {
            if let Ok(mut entity) = commands.get_entity(entity) {
                entity.remove::<Selected>();
            }
        }
    };

    if let Some(entity) = clicked_node {
        if ctrl_pressed {
            // Multi-select mode: toggle selection
            if let Some(index) = selection_set
                .selected_nodes
                .iter()
                .position(|&selected| selected == entity)
            {
                selection_set.selected_nodes.remove(index);
                commands.entity(entity).remove::<Selected>();
            } else {
                selection_set.selected_nodes.push(entity);
                commands.entity(entity).insert(Selected);
            }
        } else {
            // Single-select mode: replace the selection with the clicked node.
            clear_all(&mut commands, &mut selection_set);
            selection_set.selected_nodes.push(entity);
            commands.entity(entity).insert(Selected);
        }
    } else if !ctrl_pressed {
        // Clicked empty space without Ctrl: clear all selections
        clear_all(&mut commands, &mut selection_set);
    }
}

/// Detect mouse click on nodes and start dragging
pub fn start_node_drag(
    ui_pointer: Res<UiPointerState>,
    mouse_button: Res<ButtonInput<MouseButton>>,
    windows: Query<&Window, With<PrimaryWindow>>,
    camera_query: Query<(&Camera, &GlobalTransform), With<WorldView>>,
    mut drag_state: ResMut<DragState>,
    mut commands: Commands,
    node_query: Query<(Entity, &Transform, &NodeHitbox, Option<&Visibility>)>,
) {
    // Don't start drag if the pointer is over blocking UI
    if ui_pointer.over_ui_blocking {
        return;
    }

    // Only start drag on left mouse button press
    if !mouse_button.just_pressed(MouseButton::Left) {
        return;
    };

    let Some(world_position) = cursor_world_position(&windows, &camera_query) else {
        return;
    };

    let hit = node_at(
        world_position,
        node_query.iter().map(|(entity, transform, hitbox, vis)| {
            (
                entity,
                transform.translation.truncate(),
                hitbox.radius,
                is_visible(vis),
            )
        }),
    );

    if let Some(entity) = hit
        && let Ok((_, transform, _, _)) = node_query.get(entity)
    {
        let node_pos = transform.translation.truncate();
        drag_state.dragging_entity = Some(entity);
        drag_state.drag_offset = world_position - node_pos;
        commands.entity(entity).insert(Dragging);
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
        velocity.linear = Vec2::ZERO;
        velocity.angular = 0.0;
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
    selected_nodes: Query<(Entity, &NodeHitbox), With<Selected>>,
    selection_rings: Query<(Entity, &SelectionRing)>,
    mut meshes: ResMut<Assets<Mesh>>,
    mut materials: ResMut<Assets<ColorMaterial>>,
) {
    /// How far the ring sits outside the node's hitbox.
    const RING_MARGIN: f32 = 5.0;

    // Remove rings for nodes that are no longer selected
    for (ring_entity, selection_ring) in selection_rings.iter() {
        if !selected_nodes.contains(selection_ring.node_entity) {
            commands.entity(ring_entity).despawn();
        }
    }

    // Add rings for newly selected nodes
    for (node_entity, hitbox) in selected_nodes.iter() {
        // Check if this node already has a selection ring
        let has_ring = selection_rings
            .iter()
            .any(|(_, ring)| ring.node_entity == node_entity);

        if !has_ring {
            // Sized from the node's own hitbox, so a 64-unit account node doesn't
            // get the ring drawn for a 100-unit instance node.
            let ring = Mesh::from(Circle::new(hitbox.radius + RING_MARGIN));

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

/// Marker for the native selection-count badge (top-right corner).
#[derive(Component)]
pub struct SelectionBadge;

/// Show/update a native badge with the selected node count when more than one node
/// is selected; hide it otherwise.
pub fn update_selection_ui(
    mut commands: Commands,
    theme: Res<crate::gui::ui::theme::Theme>,
    selection_set: Res<SelectionSet>,
    badge: Query<Entity, With<SelectionBadge>>,
    mut labels: Query<&mut Text, With<SelectionBadge>>,
) {
    use crate::gui::ui::theme::{Role, ThemedBg, ThemedBorder};

    let count = selection_set.selected_nodes.len();
    if count <= 1 {
        for entity in &badge {
            commands.entity(entity).despawn();
        }
        return;
    }

    let label = format!("{} nodes selected", count);
    if let Ok(mut text) = labels.single_mut() {
        if text.0 != label {
            text.0 = label;
        }
        return;
    }

    commands.spawn((
        SelectionBadge,
        Text::new(label),
        theme.text_font(theme.metrics.font_md),
        TextColor(theme.color(Role::Text)),
        Node {
            position_type: PositionType::Absolute,
            top: Val::Px(10.0),
            right: Val::Px(10.0),
            padding: UiRect::axes(
                Val::Px(theme.metrics.space_md),
                Val::Px(theme.metrics.space_sm),
            ),
            border: UiRect::all(Val::Px(1.0)),
            ..default()
        },
        BackgroundColor(theme.color(Role::Panel)),
        ThemedBg(Role::Panel),
        BorderColor::all(theme.color(Role::Border)),
        ThemedBorder(Role::Border),
        GlobalZIndex(crate::gui::ui::z::CHROME),
    ));
}
