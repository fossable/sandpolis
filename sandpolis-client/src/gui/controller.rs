//! Node controller host.
//!
//! The host opens a draggable [`FloatingPanel`](crate::gui::ui::panel::FloatingPanel)
//! for the active layer's [`NodeController`](crate::gui::ui::controller::NodeController)
//! when a node is double-clicked (or its preview's "Open" button is pressed), and
//! despawns it on close / layer change. Layer-specific content is built by the
//! controller registered in the [`LayerRegistry`].

use crate::gui::input::CurrentLayer;
use crate::gui::node::{NodeEntity, WorldView};
use crate::gui::ui::controller::LayerRegistry;
use crate::gui::ui::gating::UiPointerState;
use crate::gui::ui::panel::{FloatingPanel, PanelClosed, spawn_floating_panel};
use crate::gui::ui::theme::Theme;
use bevy::prelude::*;
use sandpolis_instance::InstanceId;

/// Which node's controller is currently open (if any).
#[derive(Resource, Default)]
pub struct NodeControllerState {
    pub open: Option<InstanceId>,
}

impl NodeControllerState {
    /// Responsive controller dimensions based on window size. Mobile screens use
    /// most of the viewport; desktop uses a fixed size.
    pub fn get_controller_dimensions(window_width: f32, window_height: f32) -> (f32, f32) {
        if window_width < 800.0 {
            let width = (window_width * 0.95).max(280.0);
            let height = (window_height * 0.80).max(400.0);
            (width, height)
        } else {
            (600.0, 440.0)
        }
    }
}

/// Marks the controller's floating panel and records which instance it is for.
#[derive(Component)]
pub struct ControllerPanel(pub InstanceId);

/// Installs the controller host: state, panel management, and close handling.
pub struct ControllerHostPlugin;

impl Plugin for ControllerHostPlugin {
    fn build(&self, app: &mut App) {
        app.init_resource::<NodeControllerState>()
            .add_observer(on_panel_closed)
            .add_systems(Update, manage_controller)
            .add_systems(
                PostUpdate,
                (handle_node_double_click, close_controller_on_layer_change),
            );
    }
}

/// Spawn / rebuild / despawn the controller panel to match [`NodeControllerState`].
pub fn manage_controller(
    mut commands: Commands,
    theme: Res<Theme>,
    state: Res<NodeControllerState>,
    registry: Res<LayerRegistry>,
    current_layer: Res<CurrentLayer>,
    windows: Query<&Window>,
    existing: Query<(Entity, &ControllerPanel), With<FloatingPanel>>,
) {
    let current = existing.iter().next();

    match (state.open, current) {
        (Some(instance), Some((_, panel))) if panel.0 == instance => {
            // Already showing the right controller.
        }
        (None, None) => {}
        (want, current) => {
            // Despawn any stale panel.
            if let Some((entity, _)) = current {
                commands.entity(entity).despawn();
            }
            // Spawn a fresh one if requested.
            if let Some(instance) = want {
                let Some(info) = registry.get(&current_layer) else {
                    return;
                };
                let Some(controller) = info.controller.clone() else {
                    return;
                };
                let (win_w, win_h) = windows
                    .single()
                    .map(|w| (w.width(), w.height()))
                    .unwrap_or((1280.0, 720.0));
                let (w, h) = NodeControllerState::get_controller_dimensions(win_w, win_h);
                let pos = Vec2::new((win_w - w) / 2.0, (win_h - h) / 2.0);
                let panel =
                    spawn_floating_panel(&mut commands, &theme, controller.title(), pos, Vec2::new(w, h));
                commands.entity(panel.root).insert(ControllerPanel(instance));
                controller.build(&mut commands, panel.body, instance, &theme);
            }
        }
    }
}

/// Close the controller when its panel's close button is clicked. Other
/// floating panels (e.g. the database browser) handle their own `PanelClosed`.
fn on_panel_closed(
    closed: On<PanelClosed>,
    panels: Query<&ControllerPanel>,
    mut state: ResMut<NodeControllerState>,
) {
    if panels.contains(closed.entity) {
        state.open = None;
    }
}

/// Detect a double-click on a node to toggle its controller.
pub fn handle_node_double_click(
    ui_pointer: Res<UiPointerState>,
    mouse_button: Res<ButtonInput<MouseButton>>,
    time: Res<Time>,
    windows: Query<&Window>,
    camera_query: Query<(&Camera, &GlobalTransform), With<WorldView>>,
    node_query: Query<(&Transform, &NodeEntity)>,
    mut controller_state: ResMut<NodeControllerState>,
    mut last_click: Local<(f32, Option<InstanceId>)>,
) {
    if ui_pointer.over_ui_blocking {
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

    const CLICK_RADIUS: f32 = 50.0;
    let mut clicked_node: Option<InstanceId> = None;
    for (transform, node_entity) in node_query.iter() {
        if world_position.distance(transform.translation.truncate()) <= CLICK_RADIUS {
            clicked_node = Some(node_entity.instance_id);
            break;
        }
    }

    let current_time = time.elapsed_secs();
    let (last_time, last_entity) = *last_click;
    if let Some(clicked_id) = clicked_node {
        if current_time - last_time < 0.3 && last_entity == Some(clicked_id) {
            controller_state.open = if controller_state.open == Some(clicked_id) {
                None
            } else {
                Some(clicked_id)
            };
        }
        *last_click = (current_time, Some(clicked_id));
    }
}

/// Close the controller when switching layers.
pub fn close_controller_on_layer_change(
    current_layer: Res<CurrentLayer>,
    mut controller_state: ResMut<NodeControllerState>,
) {
    if current_layer.is_changed() && !current_layer.is_added() {
        controller_state.open = None;
    }
}
