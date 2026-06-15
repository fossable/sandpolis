//! Minimap: a small bottom-right overview of the node graph with a rectangle
//! showing the current camera viewport.
//!
//! Migrated off egui's `ui.painter()` to native `bevy_ui`: the minimap is a panel
//! node, each world node is drawn as a small absolutely-positioned dot child, and
//! the viewport rectangle is a bordered child node. Dots are rebuilt each frame
//! (cheap for the node counts involved); the viewport rect is repositioned.

use crate::gui::node::{NodeEntity, WorldView};
use crate::gui::ui::gating::BlocksWorldInput;
use crate::gui::ui::z;
use bevy::prelude::*;

/// Half-extent of the fixed world area shown by the minimap, in world units.
const FIXED_WORLD: f32 = 2000.0;

/// Marker for the minimap panel.
#[derive(Component)]
pub struct Minimap;

/// Marker for a node dot inside the minimap.
#[derive(Component)]
pub struct MinimapDot;

/// Marker for the camera-viewport rectangle inside the minimap.
#[derive(Component)]
pub struct MinimapViewportRect;

/// Resource controlling minimap size and placement.
#[derive(Resource)]
pub struct MinimapViewport {
    pub width: f32,
    pub height: f32,
    /// Offset from the bottom-right corner of the window.
    pub bottom_right_offset: Vec2,
}

impl MinimapViewport {
    /// Responsive minimap dimensions based on window size.
    pub fn from_window_size(window_width: f32, window_height: f32) -> Self {
        let is_mobile = window_width < 800.0;
        if is_mobile {
            Self {
                width: (window_width * 0.25).max(120.0),
                height: (window_height * 0.15).max(90.0),
                bottom_right_offset: Vec2::new(5.0, 5.0),
            }
        } else {
            Self::default()
        }
    }
}

impl Default for MinimapViewport {
    fn default() -> Self {
        Self {
            width: 200.0,
            height: 150.0,
            bottom_right_offset: Vec2::new(10.0, 10.0),
        }
    }
}

/// Spawn the minimap panel and its (initially empty) viewport rectangle.
pub fn spawn_minimap(mut commands: Commands, viewport: Res<MinimapViewport>) {
    commands.spawn((
        Minimap,
        BlocksWorldInput,
        GlobalZIndex(z::CHROME),
        Node {
            position_type: PositionType::Absolute,
            right: Val::Px(viewport.bottom_right_offset.x),
            bottom: Val::Px(viewport.bottom_right_offset.y),
            width: Val::Px(viewport.width),
            height: Val::Px(viewport.height),
            border: UiRect::all(Val::Px(1.0)),
            overflow: Overflow::clip(),
            ..default()
        },
        BackgroundColor(Color::srgb_u8(20, 20, 20).with_alpha(0.78)),
        BorderColor::all(Color::srgb_u8(60, 60, 60)),
        children![(
            MinimapViewportRect,
            Node {
                position_type: PositionType::Absolute,
                left: Val::Px(0.0),
                top: Val::Px(0.0),
                width: Val::Px(0.0),
                height: Val::Px(0.0),
                border: UiRect::all(Val::Px(1.0)),
                ..default()
            },
            BorderColor::all(Color::srgb_u8(255, 200, 0)),
        )],
    ));
}

/// Rebuild node dots and reposition the viewport rectangle each frame.
pub fn update_minimap(
    mut commands: Commands,
    viewport: Res<MinimapViewport>,
    panel_query: Query<Entity, With<Minimap>>,
    dots_query: Query<Entity, With<MinimapDot>>,
    nodes_query: Query<&Transform, With<NodeEntity>>,
    camera_query: Query<(&Transform, &Projection), With<WorldView>>,
    mut rect_query: Query<&mut Node, With<MinimapViewportRect>>,
) {
    let Ok(panel) = panel_query.single() else {
        return;
    };

    // Rebuild dots from scratch (node counts are small).
    for dot in dots_query.iter() {
        commands.entity(dot).despawn();
    }

    let world_span = FIXED_WORLD * 2.0;
    let scale = (viewport.width / world_span).min(viewport.height / world_span) * 0.9;
    // World (Y up) to minimap-local pixels (Y down), relative to the panel.
    let to_local = |wx: f32, wy: f32| -> (f32, f32) {
        ((wx + FIXED_WORLD) * scale, (FIXED_WORLD - wy) * scale)
    };

    for transform in nodes_query.iter() {
        let pos = transform.translation;
        let (lx, ly) = to_local(pos.x, pos.y);
        commands.entity(panel).with_children(|parent| {
            parent.spawn((
                MinimapDot,
                Node {
                    position_type: PositionType::Absolute,
                    left: Val::Px(lx - 3.0),
                    top: Val::Px(ly - 3.0),
                    width: Val::Px(6.0),
                    height: Val::Px(6.0),
                    ..default()
                },
                BackgroundColor(Color::srgb_u8(100, 150, 255)),
            ));
        });
    }

    // Reposition the camera viewport rectangle.
    if let Ok((cam_tf, Projection::Orthographic(ortho))) = camera_query.single() {
        let cam = cam_tf.translation.truncate();
        let half = Vec2::new(
            ortho.area.width() * ortho.scale / 2.0,
            ortho.area.height() * ortho.scale / 2.0,
        );
        let (lx, ly) = to_local(cam.x - half.x, cam.y + half.y);
        let (rx, ry) = to_local(cam.x + half.x, cam.y - half.y);
        if let Ok(mut node) = rect_query.single_mut() {
            node.left = Val::Px(lx.max(0.0));
            node.top = Val::Px(ly.max(0.0));
            node.width = Val::Px((rx - lx).max(0.0));
            node.height = Val::Px((ry - ly).max(0.0));
        }
    }
}
