//! Node controller host.
//!
//! The host opens a draggable [`FloatingPanel`](crate::gui::ui::panel::FloatingPanel)
//! for the active layer's [`NodeController`](crate::gui::ui::controller::NodeController)
//! when a node is double-clicked (or its preview's "Open" button is pressed), and
//! despawns it on close / layer change. Layer-specific content is built by the
//! controller registered in the [`LayerRegistry`].

use crate::gui::drag::{cursor_world_position, is_visible, node_at};
use crate::gui::input::CurrentLayer;
use crate::gui::node::{NodeEntity, NodeHitbox, SubNode, WorldView};
use crate::gui::ui::controller::LayerRegistry;
use crate::gui::ui::gating::UiPointerState;
use crate::gui::ui::panel::{FloatingPanel, PanelClosed, spawn_floating_panel};
use crate::gui::ui::theme::Theme;
use bevy::prelude::*;
use bevy::window::PrimaryWindow;
use sandpolis_instance::InstanceId;

/// What an open controller describes.
///
/// Most nodes *are* an instance, so `sub` is `None`. Nodes carrying a
/// [`SubNode`] stand in for something finer-grained that borrows the instance's
/// id — a probe node and its gateway server share an `InstanceId`, and only the
/// sub key separates them.
#[derive(Clone, Copy, PartialEq, Eq)]
pub struct ControllerTarget {
    pub instance: InstanceId,
    /// The clicked [`SubNode`]'s id, when the node wasn't an instance itself.
    pub sub: Option<u64>,
}

impl ControllerTarget {
    /// A target for a plain instance node.
    pub fn instance(instance: InstanceId) -> Self {
        Self {
            instance,
            sub: None,
        }
    }
}

/// Which node's controller is currently open (if any).
#[derive(Resource, Default)]
pub struct NodeControllerState {
    pub open: Option<ControllerTarget>,
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

/// Marks the controller's floating panel and records what it is for.
#[derive(Component)]
pub struct ControllerPanel(pub ControllerTarget);

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
        (Some(target), Some((_, panel))) if panel.0 == target => {
            // Already showing the right controller.
        }
        (None, None) => {}
        (want, current) => {
            // Despawn any stale panel.
            if let Some((entity, _)) = current {
                commands.entity(entity).despawn();
            }
            // Spawn a fresh one if requested.
            if let Some(target) = want {
                let Some(info) = registry.get(&current_layer) else {
                    return;
                };
                let Some(controller) = info.controller.clone() else {
                    return;
                };
                // Backstop for entry points other than the double-click handler
                // (the preview card's "Open" button targets an instance).
                if target.sub.is_none() && !info.controller_on_instance_nodes {
                    return;
                }
                let (win_w, win_h) = windows
                    .single()
                    .map(|w| (w.width(), w.height()))
                    .unwrap_or((1280.0, 720.0));
                let (w, h) = NodeControllerState::get_controller_dimensions(win_w, win_h);
                let pos = Vec2::new((win_w - w) / 2.0, (win_h - h) / 2.0);
                let panel = spawn_floating_panel(
                    &mut commands,
                    &theme,
                    controller.title_for(target),
                    pos,
                    Vec2::new(w, h),
                );
                commands.entity(panel.root).insert(ControllerPanel(target));
                controller.build(&mut commands, panel.body, target, &theme);
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
    registry: Res<LayerRegistry>,
    current_layer: Res<CurrentLayer>,
    windows: Query<&Window, With<PrimaryWindow>>,
    camera_query: Query<(&Camera, &GlobalTransform), With<WorldView>>,
    node_query: Query<(
        Entity,
        &Transform,
        &NodeEntity,
        &NodeHitbox,
        Option<&Visibility>,
        Option<&SubNode>,
    )>,
    mut controller_state: ResMut<NodeControllerState>,
    mut last_click: Local<(f32, Option<Entity>)>,
) {
    if ui_pointer.over_ui_blocking {
        return;
    }
    if !mouse_button.just_pressed(MouseButton::Left) {
        return;
    }

    let Some(world_position) = cursor_world_position(&windows, &camera_query) else {
        return;
    };

    // Nearest hitbox wins rather than the first match: probe nodes orbit close
    // enough to their gateway that a fixed radius would keep hitting the server.
    let clicked = node_at(
        world_position,
        node_query
            .iter()
            .map(|(entity, transform, _, hitbox, vis, _)| {
                (
                    entity,
                    transform.translation.truncate(),
                    hitbox.radius,
                    is_visible(vis),
                )
            }),
    );

    let current_time = time.elapsed_secs();
    let (last_time, last_entity) = *last_click;
    let Some(entity) = clicked else {
        return;
    };

    if current_time - last_time < 0.3 && last_entity == Some(entity) {
        let Ok((_, _, node_entity, _, _, sub)) = node_query.get(entity) else {
            return;
        };
        // Layers whose controller describes a sub-node don't open one for the
        // plain instance nodes their sub-nodes hang off.
        let allowed = sub.is_some()
            || registry
                .get(&current_layer)
                .map(|info| info.controller_on_instance_nodes)
                .unwrap_or(true);
        if allowed {
            let target = ControllerTarget {
                instance: node_entity.instance_id,
                sub: sub.map(|s| s.0),
            };
            controller_state.open = if controller_state.open == Some(target) {
                None
            } else {
                Some(target)
            };
        }
    }
    *last_click = (current_time, Some(entity));
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
