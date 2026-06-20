//! Mouse-wheel scrolling for `bevy_ui` nodes.
//!
//! `bevy_ui` updates a node's child offset from its [`ScrollPosition`] but does
//! not wire mouse-wheel input to it, so a node with `Overflow::scroll_*` won't
//! actually scroll on its own. This installs the small bridge (lifted from Bevy's
//! `ui/scroll_and_overflow` example): wheel events over a hovered node are turned
//! into [`Scroll`] events that adjust the nearest scrollable ancestor's
//! [`ScrollPosition`].

use bevy::input::mouse::{MouseScrollUnit, MouseWheel};
use bevy::picking::hover::HoverMap;
use bevy::prelude::*;

/// One wheel "line" in logical pixels, used to scale line-unit scroll deltas.
const LINE_HEIGHT: f32 = 21.0;

/// Installs wheel-driven scrolling for nodes with `Overflow::scroll_*`.
pub struct ScrollPlugin;

impl Plugin for ScrollPlugin {
    fn build(&self, app: &mut App) {
        app.add_systems(Update, send_scroll_events)
            .add_observer(on_scroll_handler);
    }
}

/// A wheel-scroll request bubbling up the UI tree until a scrollable node
/// consumes it.
#[derive(EntityEvent, Debug)]
#[entity_event(propagate, auto_propagate)]
struct Scroll {
    entity: Entity,
    /// Scroll delta in logical coordinates.
    delta: Vec2,
}

/// Turn mouse-wheel input over hovered entities into bubbling [`Scroll`] events.
fn send_scroll_events(
    mut wheel: MessageReader<MouseWheel>,
    hover_map: Res<HoverMap>,
    mut commands: Commands,
) {
    for event in wheel.read() {
        let mut delta = -Vec2::new(event.x, event.y);
        if event.unit == MouseScrollUnit::Line {
            delta *= LINE_HEIGHT;
        }
        for pointer_map in hover_map.values() {
            for entity in pointer_map.keys().copied() {
                commands.trigger(Scroll { entity, delta });
            }
        }
    }
}

/// Apply a [`Scroll`] to the first scrollable node on its bubble path.
fn on_scroll_handler(
    mut scroll: On<Scroll>,
    mut query: Query<(&mut ScrollPosition, &Node, &ComputedNode)>,
) {
    let Ok((mut scroll_position, node, computed)) = query.get_mut(scroll.entity) else {
        return;
    };

    let max_offset = (computed.content_size() - computed.size()) * computed.inverse_scale_factor();
    let delta = &mut scroll.delta;

    if node.overflow.x == OverflowAxis::Scroll && delta.x != 0.0 {
        let at_limit = if delta.x > 0.0 {
            scroll_position.x >= max_offset.x
        } else {
            scroll_position.x <= 0.0
        };
        if !at_limit {
            scroll_position.x += delta.x;
            delta.x = 0.0;
        }
    }

    if node.overflow.y == OverflowAxis::Scroll && delta.y != 0.0 {
        let at_limit = if delta.y > 0.0 {
            scroll_position.y >= max_offset.y
        } else {
            scroll_position.y <= 0.0
        };
        if !at_limit {
            scroll_position.y += delta.y;
            delta.y = 0.0;
        }
    }

    if *delta == Vec2::ZERO {
        scroll.propagate(false);
    }
}
