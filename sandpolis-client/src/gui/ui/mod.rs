//! Native `bevy_ui` abstraction layer for the Sandpolis GUI.
//!
//! This is the foundation for migrating the GUI off `egui` onto native
//! `bevy_ui` + `bevy_ui_widgets`. It provides:
//!
//! - [`theme`]: a palette/metrics theming system with live re-application.
//! - [`gating`]: a [`gating::UiPointerState`] resource so world systems know when
//!   the pointer/keyboard is captured by the UI (replaces egui's
//!   `wants_pointer_input` / `is_pointer_over_area` checks).
//! - [`icon`]: SVG-to-texture rasterization for icons inside UI nodes.
//! - [`widgets`]: themed spawn-helpers built on top of `bevy_ui_widgets`.
//!
//! Add [`UiPlugin`] to install everything.

use bevy::prelude::*;

pub mod anchored;
pub mod gating;
pub mod icon;
pub mod panel;
pub mod text_input;
pub mod theme;
pub mod widgets;

/// `GlobalZIndex` tiers. `bevy_ui` does not auto-stack overlapping UI the way egui
/// does, so every floating element is assigned an explicit tier.
pub mod z {
    /// World-anchored cards that track nodes (previews, edge labels).
    pub const ANCHORED: i32 = 100;
    /// Always-on chrome (minimap, layer indicator).
    pub const CHROME: i32 = 200;
    /// Floating panels (node controllers).
    pub const PANEL: i32 = 300;
    /// Modal scrim + dialogs.
    pub const MODAL: i32 = 400;
    /// Transient popups (dropdowns, tooltips).
    pub const POPUP: i32 = 500;
}

/// Installs the native UI foundation: the `bevy_ui_widgets` headless widget
/// observers, theming, input-gating, and icon rasterization.
pub struct UiPlugin;

impl Plugin for UiPlugin {
    fn build(&self, app: &mut App) {
        // `bevy_ui_widgets` relies on the input-focus resource/dispatch and tab
        // navigation, which `DefaultPlugins` does not add on its own.
        app.add_plugins((
            bevy::input_focus::InputDispatchPlugin,
            bevy::input_focus::tab_navigation::TabNavigationPlugin,
        ))
        .add_plugins(bevy_ui_widgets::UiWidgetsPlugins)
            .add_plugins(theme::ThemePlugin)
            .add_plugins(gating::GatingPlugin)
            .add_plugins(icon::IconPlugin)
            .add_plugins(text_input::TextInputPlugin)
            .add_plugins(anchored::AnchoredPlugin);
    }
}
