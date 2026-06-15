//! Structural containers: modal overlays and floating panels.
//!
//! `bevy_ui` has no window/dialog concept, so these provide the egui `Window`
//! affordances we rely on. For now: a full-screen modal scrim that centers its
//! content and blocks the world beneath. (Draggable `FloatingPanel` lands with the
//! node controller in a later phase.)

use super::gating::BlocksWorldInput;
use super::z;
use bevy::prelude::*;

/// A full-screen modal scrim that centers its child content and blocks the world
/// beneath it. Spawn the dialog body as a child.
pub fn modal_scrim() -> impl Bundle {
    (
        Node {
            position_type: PositionType::Absolute,
            left: Val::Px(0.0),
            top: Val::Px(0.0),
            right: Val::Px(0.0),
            bottom: Val::Px(0.0),
            align_items: AlignItems::Center,
            justify_content: JustifyContent::Center,
            ..default()
        },
        BackgroundColor(Color::BLACK.with_alpha(0.55)),
        BlocksWorldInput,
        GlobalZIndex(z::MODAL),
    )
}
