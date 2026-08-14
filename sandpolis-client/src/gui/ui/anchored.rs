//! World-anchored UI: position a UI node so it tracks a world entity on screen.
//!
//! Used for node panels (and, later, anything else that floats next to a graph
//! node).
//! The node's [`Node::left`]/[`Node::top`] are set from the target's projected
//! screen position each frame; the node is hidden when the target is off-screen.

use crate::gui::node::WorldView;
use crate::gui::ui::theme::{Role, Theme, ThemedBg, ThemedBorder};
use crate::gui::ui::z;
use bevy::image::Image;
use bevy::prelude::*;
use bevy::window::PrimaryWindow;

/// Anchor a UI node to a world entity's on-screen position, plus a screen-space
/// offset. The node should use `position_type: Absolute`.
#[derive(Component)]
pub struct WorldAnchored {
    /// The world entity to follow.
    pub target: Entity,
    /// Screen-space offset applied after centering horizontally on the target.
    pub offset: Vec2,
    /// Keep the node fully on screen, sliding it back inside the viewport when
    /// following the target exactly would push it out.
    ///
    /// Off for small cards, where a few pixels of drift would read as the card
    /// having come loose from its node; on for anything large enough that
    /// running off the edge would hide real content.
    pub clamp: bool,
}

/// The chrome shared by every card that floats beside a world entity: an
/// absolutely positioned, themed row anchored under `target`.
///
/// Collapsed node panels build on this, so every kind of node gets the same card
/// without each layer rebuilding it. `padding` is a parameter because a card
/// shrinks with the world view's zoom, and the border and fill should not.
pub fn anchored_card(theme: &Theme, target: Entity, offset: Vec2, padding: f32) -> impl Bundle {
    (
        WorldAnchored {
            target,
            offset,
            clamp: false,
        },
        GlobalZIndex(z::ANCHORED),
        Node {
            position_type: PositionType::Absolute,
            flex_direction: FlexDirection::Row,
            align_items: AlignItems::Center,
            column_gap: Val::Px(padding),
            padding: UiRect::all(Val::Px(padding)),
            border: UiRect::all(Val::Px(1.0)),
            ..default()
        },
        BackgroundColor(theme.color(Role::Panel)),
        ThemedBg(Role::Panel),
        BorderColor::all(theme.color(Role::Border)),
        ThemedBorder(Role::Border),
    )
}

/// A square icon sized for an [`anchored_card`].
pub fn card_icon(image: Handle<Image>, px: f32) -> impl Bundle {
    (
        ImageNode::new(image),
        Node {
            width: Val::Px(px),
            height: Val::Px(px),
            ..default()
        },
    )
}

/// Plugin that runs [`update_world_anchored`] before UI layout.
pub struct AnchoredPlugin;

impl Plugin for AnchoredPlugin {
    fn build(&self, app: &mut App) {
        app.add_systems(
            PostUpdate,
            update_world_anchored.before(bevy::ui::UiSystems::Layout),
        );
    }
}

/// Project each anchored node's target to screen space and reposition it.
pub fn update_world_anchored(
    camera: Query<(&Camera, &GlobalTransform), With<WorldView>>,
    windows: Query<&Window, With<PrimaryWindow>>,
    targets: Query<&GlobalTransform>,
    mut anchored: Query<(&WorldAnchored, &mut Node, &mut Visibility, &ComputedNode)>,
) {
    let Ok((camera, camera_transform)) = camera.single() else {
        return;
    };
    let viewport = windows
        .single()
        .ok()
        .map(|window| Vec2::new(window.width(), window.height()));

    for (anchor, mut node, mut visibility, computed) in &mut anchored {
        let Ok(target) = targets.get(anchor.target) else {
            *visibility = Visibility::Hidden;
            continue;
        };
        match camera.world_to_viewport(camera_transform, target.translation()) {
            Ok(screen) => {
                let size = computed.size() * computed.inverse_scale_factor();
                let mut position = Vec2::new(
                    screen.x - size.x / 2.0 + anchor.offset.x,
                    screen.y + anchor.offset.y,
                );
                // Clamped after offsetting, so a panel whose node sits at the edge
                // slides inward instead of hanging half off the window. `max(0.0)`
                // on the limit keeps a node larger than the viewport pinned to the
                // top-left rather than flipped past it.
                if anchor.clamp
                    && let Some(viewport) = viewport
                {
                    let margin = Vec2::splat(8.0);
                    let limit = (viewport - size - margin).max(margin);
                    position = position.clamp(margin, limit);
                }
                node.left = Val::Px(position.x);
                node.top = Val::Px(position.y);
                *visibility = Visibility::Inherited;
            }
            Err(_) => *visibility = Visibility::Hidden,
        }
    }
}
