//! Bevy 0.19 `bsn!` scene authoring for the Sandpolis GUI.
//!
//! Bevy 0.19 introduced next-generation scenes: the [`bsn!`] macro builds an
//! entity tree declaratively, and [`Commands::spawn_scene`] spawns it. This module
//! establishes the conventions for using it alongside the codebase's existing
//! retained-mode theming, and provides the first scenes migrated to it.
//!
//! # Pattern
//!
//! - **Spawn** a scene with `commands.spawn_scene(my_scene(&theme))`. The returned
//!   `EntityCommands` lets you `.insert(...)` extra components on the root (handy
//!   for marker components that don't implement `Clone`/`Default`, which `bsn!`
//!   requires).
//! - **Theme colors** are captured into locals and embedded with `{...}`:
//!   `let panel = theme.color(Role::Panel); bsn! { BackgroundColor({panel}) }`.
//!   Always also attach the matching `Themed*` marker ([`ThemedBg`],
//!   [`ThemedText`], [`ThemedBorder`]) so the recolor systems in
//!   [`super::theme`] repaint the node when the theme preset changes.
//! - **Runtime component values** (anything built by a call, e.g. a [`TextFont`]
//!   from [`Theme::text_font`], or `BorderColor::all(..)`) are inserted with
//!   [`template_value(..)`](template_value), which works for any `Clone + Default`
//!   component.
//! - **Behavior** is attached with [`on(..)`](on), which registers an entity
//!   observer for an [`EntityEvent`] (e.g. `on(on_help_close)` for an
//!   `On<Activate>` handler).
//! - **Dynamic children** are a `Vec<impl Scene>` spliced into a `Children [..]`
//!   list with `{...}`.
//!
//! # What is *not* migrated here
//!
//! Scenes that must hand back child entity ids synchronously at spawn time — most
//! notably [`super::panel::spawn_floating_panel`], whose callers spawn controller
//! content into the returned `body` entity — don't fit the `spawn_scene` model
//! cleanly and are intentionally left imperative. The remaining modal/dialog
//! scenes are tracked for migration in `CLAUDE.md`.

use super::theme::{Role, Theme, ThemedBg, ThemedBorder, ThemedText};
use super::z;
use crate::gui::input::on_help_close;
use bevy::prelude::*;
use bevy_ui_widgets::Button;

/// A single themed text line.
fn text_line(theme: &Theme, content: impl Into<String>, role: Role, size: f32) -> impl Scene {
    let content = content.into();
    let color = theme.color(role);
    // `TextFont` isn't `Default` (so not usable via `template_value`); patch the
    // size with field syntax. A custom `Theme::font` isn't applied here — the
    // default font is used, which matches the app's default configuration.
    bsn! {
        template_value(Text::new(content))
        TextFont { font_size: {size} }
        TextColor({color})
        ThemedText({role})
    }
}

/// A themed button whose `On<Activate>` is handled by [`on_help_close`].
fn help_close_button(theme: &Theme) -> impl Scene {
    let label = text_line(theme, "Close", Role::Text, theme.metrics.font_md);
    let surface = theme.color(Role::Surface);
    let border = theme.color(Role::Border);
    let pad_x = theme.metrics.space_md;
    let pad_y = theme.metrics.space_sm;
    bsn! {
        Button
        super::theme::ThemedButton
        template_value(Interaction::default())
        Node {
            padding: {UiRect::axes(Val::Px(pad_x), Val::Px(pad_y))},
            align_items: AlignItems::Center,
            justify_content: JustifyContent::Center,
            border: {UiRect::all(Val::Px(1.0))},
        }
        BackgroundColor({surface})
        template_value(BorderColor::all(border))
        ThemedBorder({Role::Border})
        on(on_help_close)
        Children [ {vec![label]} ]
    }
}

/// The keyboard-shortcuts help modal, built with `bsn!`.
///
/// Spawn with `commands.spawn_scene(help_modal_scene(&theme, SHORTCUTS))` and then
/// `.insert((HelpRoot, BlocksWorldInput))` on the returned root (those markers
/// don't derive `Clone`, so they're added imperatively rather than in `bsn!`).
pub fn help_modal_scene(theme: &Theme, shortcuts: &[&str]) -> impl Scene {
    let scrim = Color::BLACK.with_alpha(0.55);
    let panel = theme.color(Role::Panel);
    let border = theme.color(Role::Border);

    let heading = text_line(
        theme,
        "Keyboard Shortcuts",
        Role::Text,
        theme.metrics.font_heading,
    );
    let lines: Vec<_> = shortcuts
        .iter()
        .map(|line| text_line(theme, *line, Role::TextMuted, theme.metrics.font_md))
        .collect();
    let close = help_close_button(theme);

    bsn! {
        Node {
            position_type: PositionType::Absolute,
            left: {Val::Px(0.0)},
            top: {Val::Px(0.0)},
            right: {Val::Px(0.0)},
            bottom: {Val::Px(0.0)},
            align_items: AlignItems::Center,
            justify_content: JustifyContent::Center,
        }
        BackgroundColor({scrim})
        GlobalZIndex({z::MODAL})
        Children [
            (
                Node {
                    flex_direction: FlexDirection::Column,
                    width: {Val::Px(360.0)},
                    padding: {UiRect::all(Val::Px(16.0))},
                    row_gap: {Val::Px(6.0)},
                    border: {UiRect::all(Val::Px(1.0))},
                }
                BackgroundColor({panel})
                ThemedBg({Role::Panel})
                template_value(BorderColor::all(border))
                ThemedBorder({Role::Border})
                Children [
                    {vec![heading]},
                    {lines},
                    {vec![close]},
                ]
            )
        ]
    }
}
