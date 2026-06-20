//! Input gating for the native GUI.
//!
//! egui exposed `wants_pointer_input()` / `is_pointer_over_area()` /
//! `wants_keyboard_input()` so world systems could avoid stealing input that the
//! UI wanted. With native `bevy_ui` there is no equivalent, so we derive an
//! equivalent [`UiPointerState`] from `bevy_picking` hover data and the input
//! focus.
//!
//! Tag the *root* of any UI element that should block world input with
//! [`BlocksWorldInput`]; world-anchored decorations (node previews, edge labels)
//! should NOT be tagged (and should be `Pickable::IGNORE`) so the world stays
//! interactive beneath them. Tag focusable text entry with [`WantsKeyboard`].

use bevy::input_focus::InputFocus;
use bevy::picking::hover::HoverMap;
use bevy::prelude::*;

/// Whether the UI is currently capturing pointer / keyboard input. World systems
/// (camera pan, zoom, node selection, hotkeys) should consult this.
#[derive(Resource, Default, Debug)]
pub struct UiPointerState {
    /// The pointer is over a UI element that blocks the world beneath it.
    pub over_ui_blocking: bool,
    /// A focused text entry wants keyboard input.
    pub wants_keyboard: bool,
}

/// Tag the root of a UI element that should block world pointer input.
#[derive(Component, Default)]
pub struct BlocksWorldInput;

/// Tag a focused entity (e.g. a text field) that wants keyboard input.
#[derive(Component, Default)]
pub struct WantsKeyboard;

/// System set for [`update_pointer_state`]; world-input systems should run after
/// this set so they see up-to-date gating.
#[derive(SystemSet, Debug, Clone, PartialEq, Eq, Hash)]
pub struct UiGatingSet;

/// Run condition: the pointer is over blocking UI.
pub fn pointer_over_ui(state: Res<UiPointerState>) -> bool {
    state.over_ui_blocking
}

/// Run condition: the UI wants keyboard input.
pub fn ui_wants_keyboard(state: Res<UiPointerState>) -> bool {
    state.wants_keyboard
}

/// Installs the gating resource and update system.
pub struct GatingPlugin;

impl Plugin for GatingPlugin {
    fn build(&self, app: &mut App) {
        app.init_resource::<UiPointerState>()
            .add_systems(Update, update_pointer_state.in_set(UiGatingSet));
    }
}

/// Recompute [`UiPointerState`] from picking hover data and input focus.
pub fn update_pointer_state(
    hover_map: Res<HoverMap>,
    blocking: Query<(), With<BlocksWorldInput>>,
    parents: Query<&ChildOf>,
    input_focus: Option<Res<InputFocus>>,
    keyboard_capturers: Query<(), With<WantsKeyboard>>,
    mut state: ResMut<UiPointerState>,
) {
    let mut over = false;
    'outer: for hits in hover_map.0.values() {
        for &hovered in hits.keys() {
            // Walk up the hierarchy: a hit on any child of a blocking root blocks.
            let mut current = hovered;
            loop {
                if blocking.contains(current) {
                    over = true;
                    break 'outer;
                }
                match parents.get(current) {
                    Ok(child_of) => current = child_of.parent(),
                    Err(_) => break,
                }
            }
        }
    }
    state.over_ui_blocking = over;

    state.wants_keyboard = input_focus
        .and_then(|focus| focus.get())
        .map(|entity| keyboard_capturers.contains(entity))
        .unwrap_or(false);
}
