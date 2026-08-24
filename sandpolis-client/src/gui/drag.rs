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

/// How far the pointer may travel between press and release and still count as a
/// click rather than a drag, in logical pixels.
const CLICK_SLOP: f32 = 4.0;

/// A press that hasn't come up yet, and what was under it.
pub struct PendingPress {
    /// Screen position of the press, to measure travel against.
    origin: Vec2,
    /// The node under the press, if any.
    node: Option<Entity>,
}

/// Whether the pointer travelled far enough between `origin` and `cursor` to
/// count as a drag rather than a click.
fn is_drag(origin: Vec2, cursor: Vec2) -> bool {
    cursor.distance(origin) > CLICK_SLOP
}

/// Handle node selection on click (single-click to select, Ctrl-click to
/// multi-select).
///
/// Selection is decided on *release*, and only when the pointer stayed within
/// [`CLICK_SLOP`] of where it went down. Deciding on press would mean grabbing a
/// node to move it also selected it — which now expands its panel and opens
/// whatever stream that panel shows, so dragging a node would start a shell
/// session. Releasing after a drag leaves the selection exactly as it was, which
/// also stops a camera pan from clearing it.
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
    mut pending: Local<Option<PendingPress>>,
) {
    let cursor = windows
        .single()
        .ok()
        .and_then(|window| window.cursor_position());

    if mouse_button.just_pressed(MouseButton::Left) {
        // A press that starts over blocking UI belongs to that UI, so nothing is
        // recorded and the release below finds nothing to do.
        *pending = match (ui_pointer.over_ui_blocking, cursor) {
            (false, Some(origin)) => Some(PendingPress {
                origin,
                // Hit-tested now rather than on release: if this turns out to be
                // a drag, the node will have moved out from under the pointer.
                node: cursor_world_position(&windows, &camera_query).and_then(|world_position| {
                    node_at(
                        world_position,
                        node_query.iter().map(|(entity, transform, hitbox, vis)| {
                            (
                                entity,
                                transform.translation.truncate(),
                                hitbox.radius,
                                is_visible(vis),
                            )
                        }),
                    )
                }),
            }),
            _ => None,
        };
    }

    if !mouse_button.just_released(MouseButton::Left) {
        return;
    }
    let Some(press) = pending.take() else {
        return;
    };
    // No cursor to compare against (it left the window): treat it as a drag and
    // leave the selection alone rather than guessing.
    let Some(cursor) = cursor else {
        return;
    };
    if is_drag(press.origin, cursor) {
        return;
    }
    let clicked_node = press.node;

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

#[cfg(test)]
mod test_drag {
    use super::*;

    #[test]
    fn a_still_pointer_is_a_click() {
        assert!(!is_drag(Vec2::new(100.0, 100.0), Vec2::new(100.0, 100.0)));
    }

    #[test]
    fn a_shaky_hand_is_still_a_click() {
        // Without some slop, a click that moved a pixel would read as a drag and
        // silently do nothing.
        assert!(!is_drag(Vec2::new(100.0, 100.0), Vec2::new(102.0, 102.0)));
    }

    #[test]
    fn moving_the_node_is_a_drag() {
        assert!(is_drag(Vec2::new(100.0, 100.0), Vec2::new(140.0, 100.0)));
        assert!(is_drag(Vec2::new(100.0, 100.0), Vec2::new(100.0, 60.0)));
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
