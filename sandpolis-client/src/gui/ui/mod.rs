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
pub mod bind;
pub mod controller;
pub mod gating;
pub mod icon;
pub mod panel;
pub mod scene;
pub mod scroll;
pub mod text_input;
pub mod theme;
pub mod tooltip;
pub mod widgets;

/// Re-export of the `bevy_ui_widgets` activation event so layer crates can observe
/// button clicks without depending on `bevy_ui_widgets` directly.
pub use bevy_ui_widgets::Activate;

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
        // `bevy_ui_widgets` relies on the input-focus resource/dispatch, tab
        // navigation, and the headless widget plugins. Depending on the enabled
        // Bevy feature set (e.g. `bevy_dev_tools` pulls in the `bevy_ui_widgets`
        // and `bevy_input_focus` features) these may already be part of
        // `DefaultPlugins`, so only add the ones that are missing.
        if !app.is_plugin_added::<bevy::input_focus::InputDispatchPlugin>() {
            app.add_plugins(bevy::input_focus::InputDispatchPlugin);
        }
        if !app.is_plugin_added::<bevy::input_focus::tab_navigation::TabNavigationPlugin>() {
            app.add_plugins(bevy::input_focus::tab_navigation::TabNavigationPlugin);
        }
        if !app.is_plugin_added::<bevy_ui_widgets::EditableTextInputPlugin>() {
            app.add_plugins(bevy_ui_widgets::UiWidgetsPlugins);
        }
        app.add_plugins(theme::ThemePlugin)
            .add_plugins(gating::GatingPlugin)
            .add_plugins(icon::IconPlugin)
            .add_plugins(scroll::ScrollPlugin)
            .add_plugins(anchored::AnchoredPlugin)
            .add_plugins(bind::BindPlugin)
            .add_plugins(tooltip::TooltipPlugin)
            .init_resource::<controller::LayerRegistry>();
    }
}
