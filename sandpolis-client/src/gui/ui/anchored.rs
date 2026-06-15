//! World-anchored UI: position a UI node so it tracks a world entity on screen.
//!
//! Used for node previews (and, later, anything that floats next to a graph node).
//! The node's [`Node::left`]/[`Node::top`] are set from the target's projected
//! screen position each frame; the node is hidden when the target is off-screen.

use crate::gui::node::WorldView;
use bevy::prelude::*;

/// Anchor a UI node to a world entity's on-screen position, plus a screen-space
/// offset. The node should use `position_type: Absolute`.
#[derive(Component)]
pub struct WorldAnchored {
    /// The world entity to follow.
    pub target: Entity,
    /// Screen-space offset applied after centering horizontally on the target.
    pub offset: Vec2,
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
    targets: Query<&GlobalTransform>,
    mut anchored: Query<(&WorldAnchored, &mut Node, &mut Visibility, &ComputedNode)>,
) {
    let Ok((camera, camera_transform)) = camera.single() else {
        return;
    };
    for (anchor, mut node, mut visibility, computed) in &mut anchored {
        let Ok(target) = targets.get(anchor.target) else {
            *visibility = Visibility::Hidden;
            continue;
        };
        match camera.world_to_viewport(camera_transform, target.translation()) {
            Ok(screen) => {
                let half_width = computed.size().x / 2.0;
                node.left = Val::Px(screen.x - half_width + anchor.offset.x);
                node.top = Val::Px(screen.y + anchor.offset.y);
                *visibility = Visibility::Inherited;
            }
            Err(_) => *visibility = Visibility::Hidden,
        }
    }
}
