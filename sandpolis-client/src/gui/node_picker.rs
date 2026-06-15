//! Node picker: a modal search over instance nodes that recenters the camera on
//! the chosen node. Native `bevy_ui` (migrated off egui). Opened with the `N` key.

use crate::gui::node::WorldView;
use crate::gui::ui::gating::UiPointerState;
use crate::gui::ui::panel::modal_scrim;
use crate::gui::ui::text_input::{TextInput, text_input};
use crate::gui::ui::theme::{Role, Theme, ThemedBg, ThemedBorder};
use crate::gui::ui::widgets::{heading, muted, text};
use bevy::input_focus::InputFocus;
use bevy::prelude::*;
use bevy_ui_widgets::{Activate, Button};
use sandpolis_instance::InstanceId;

/// Node picker state.
#[derive(Resource, Default)]
pub struct NodePickerState {
    pub show: bool,
    pub search_query: String,
    pub selected_index: usize,
}

/// Modal root marker.
#[derive(Component)]
pub struct NodePickerRoot;

/// Search input marker.
#[derive(Component)]
pub struct NodeSearchInput;

/// Rows container marker.
#[derive(Component)]
pub struct NodeRowsContainer;

/// A single selectable node row.
#[derive(Component)]
pub struct NodeRow {
    node_entity: Entity,
}

/// Collect nodes matching the query, sorted by instance id for stable ordering.
fn collect_nodes(
    nodes: &Query<(Entity, &InstanceId)>,
    query: &str,
) -> Vec<(Entity, InstanceId)> {
    let query = query.to_lowercase();
    let mut all: Vec<(Entity, InstanceId)> = nodes
        .iter()
        .filter(|(_, id)| {
            if query.is_empty() {
                return true;
            }
            let is_server = id.is_server();
            format!("{id}").to_lowercase().contains(&query)
                || (is_server && "server".contains(&query))
                || (!is_server && "agent".contains(&query))
        })
        .map(|(entity, id)| (entity, *id))
        .collect();
    all.sort_by_key(|(_, id)| *id);
    all
}

/// Toggle the picker with the `N` key (unless a text field is focused).
pub fn handle_node_picker_toggle(
    ui_pointer: Res<UiPointerState>,
    keyboard: Res<ButtonInput<KeyCode>>,
    mut picker: ResMut<NodePickerState>,
) {
    if ui_pointer.wants_keyboard {
        return;
    }
    if keyboard.just_pressed(KeyCode::KeyN) {
        picker.show = !picker.show;
    }
}

/// Spawn the modal when shown, despawn it when hidden.
pub fn manage_node_picker(
    mut commands: Commands,
    theme: Res<Theme>,
    mut picker: ResMut<NodePickerState>,
    mut focus: ResMut<InputFocus>,
    root: Query<Entity, With<NodePickerRoot>>,
) {
    let exists = !root.is_empty();
    if picker.show && !exists {
        picker.selected_index = 0;
        commands
            .spawn((NodePickerRoot, modal_scrim()))
            .with_children(|scrim| {
                scrim
                    .spawn((
                        Node {
                            flex_direction: FlexDirection::Column,
                            width: Val::Px(400.0),
                            padding: UiRect::all(Val::Px(12.0)),
                            row_gap: Val::Px(8.0),
                            border: UiRect::all(Val::Px(1.0)),
                            ..default()
                        },
                        BackgroundColor(theme.color(Role::Panel)),
                        ThemedBg(Role::Panel),
                        BorderColor::all(theme.color(Role::Border)),
                        ThemedBorder(Role::Border),
                    ))
                    .with_children(|panel| {
                        panel.spawn(heading(&theme, "Find Node"));
                        panel.spawn((NodeSearchInput, text_input(&theme, "id, server/agent...", false)));
                        panel.spawn((
                            NodeRowsContainer,
                            Node {
                                flex_direction: FlexDirection::Column,
                                row_gap: Val::Px(4.0),
                                ..default()
                            },
                        ));
                    });
            });
    } else if !picker.show && exists {
        for entity in &root {
            commands.entity(entity).despawn();
        }
        focus.0 = None;
    }
}

/// Focus the search box as soon as it is spawned.
pub fn focus_node_search(
    inputs: Query<Entity, Added<NodeSearchInput>>,
    mut focus: ResMut<InputFocus>,
) {
    if let Ok(entity) = inputs.single() {
        focus.0 = Some(entity);
    }
}

/// Rebuild the row list when the query or selection changes.
pub fn rebuild_node_rows(
    mut commands: Commands,
    theme: Res<Theme>,
    picker: Res<NodePickerState>,
    search: Query<Ref<TextInput>, With<NodeSearchInput>>,
    nodes: Query<(Entity, &InstanceId)>,
    container: Query<Entity, With<NodeRowsContainer>>,
    rows: Query<Entity, With<NodeRow>>,
) {
    let Ok(search_input) = search.single() else {
        return;
    };
    let Ok(container_entity) = container.single() else {
        return;
    };
    if !(search_input.is_changed() || picker.is_changed()) {
        return;
    }

    for entity in &rows {
        commands.entity(entity).despawn();
    }

    let filtered = collect_nodes(&nodes, &search_input.value);
    let selected = picker.selected_index.min(filtered.len().saturating_sub(1));

    commands.entity(container_entity).with_children(|parent| {
        if filtered.is_empty() {
            parent.spawn(muted(&theme, "No matching nodes", theme.metrics.font_md));
            return;
        }
        for (index, (entity, id)) in filtered.into_iter().enumerate() {
            let role = if index == selected {
                Role::SurfaceActive
            } else {
                Role::Surface
            };
            let label = format!(
                "{} {}",
                if id.is_server() { "Server" } else { "Agent" },
                id
            );
            parent
                .spawn((
                    NodeRow { node_entity: entity },
                    Button,
                    Interaction::default(),
                    Node {
                        width: Val::Percent(100.0),
                        padding: UiRect::axes(Val::Px(8.0), Val::Px(6.0)),
                        ..default()
                    },
                    BackgroundColor(theme.color(role)),
                    children![text(&theme, label, theme.metrics.font_md, Role::Text)],
                ))
                .observe(on_node_row_click);
        }
    });
}

/// Recenter the camera on the clicked node and close the picker.
fn on_node_row_click(
    activate: On<Activate>,
    rows: Query<&NodeRow>,
    mut picker: ResMut<NodePickerState>,
    mut camera: Query<&mut Transform, With<WorldView>>,
    nodes: Query<&Transform, (With<InstanceId>, Without<WorldView>)>,
) {
    if let Ok(row) = rows.get(activate.entity) {
        center_camera(row.node_entity, &mut camera, &nodes);
        picker.show = false;
    }
}

/// Keyboard navigation: arrows move the selection, Enter jumps, Escape closes.
pub fn node_picker_keys(
    keyboard: Res<ButtonInput<KeyCode>>,
    mut picker: ResMut<NodePickerState>,
    search: Query<&TextInput, With<NodeSearchInput>>,
    nodes: Query<(Entity, &InstanceId)>,
    mut camera: Query<&mut Transform, With<WorldView>>,
    node_transforms: Query<&Transform, (With<InstanceId>, Without<WorldView>)>,
) {
    if !picker.show {
        return;
    }
    if keyboard.just_pressed(KeyCode::Escape) {
        picker.show = false;
        return;
    }

    let query = search.single().map(|t| t.value.clone()).unwrap_or_default();
    let filtered = collect_nodes(&nodes, &query);
    if filtered.is_empty() {
        return;
    }

    if keyboard.just_pressed(KeyCode::ArrowDown) {
        picker.selected_index = (picker.selected_index + 1).min(filtered.len() - 1);
    }
    if keyboard.just_pressed(KeyCode::ArrowUp) {
        picker.selected_index = picker.selected_index.saturating_sub(1);
    }
    if keyboard.just_pressed(KeyCode::Enter) {
        let index = picker.selected_index.min(filtered.len() - 1);
        center_camera(filtered[index].0, &mut camera, &node_transforms);
        picker.show = false;
    }
}

/// Move the world camera to center on `node_entity` (preserving its Z).
fn center_camera(
    node_entity: Entity,
    camera: &mut Query<&mut Transform, With<WorldView>>,
    nodes: &Query<&Transform, (With<InstanceId>, Without<WorldView>)>,
) {
    if let (Ok(mut cam), Ok(node)) = (camera.single_mut(), nodes.get(node_entity)) {
        cam.translation.x = node.translation.x;
        cam.translation.y = node.translation.y;
    }
}
