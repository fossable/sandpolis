//! Themed widget spawn-helpers built on `bevy_ui` + `bevy_ui_widgets`.
//!
//! These return `impl Bundle` so they compose with `children![]` and
//! `commands.spawn(...)`. Colors come from the [`Theme`] at spawn time and are
//! kept in sync afterward by the recolor systems via the `Themed*` markers.
//!
//! Heavier widgets (text input, scroll view, dropdown, floating panel, modal,
//! list view) are added in later migration phases as their consumers arrive.

use super::theme::{Role, Theme, ThemedBorder, ThemedButton, ThemedText};
use bevy::prelude::*;
use bevy_ui_widgets::Button;

/// A text node in the given role/size.
pub fn text(theme: &Theme, content: impl Into<String>, size: f32, role: Role) -> impl Bundle {
    (
        Text::new(content),
        theme.text_font(size),
        TextColor(theme.color(role)),
        ThemedText(role),
    )
}

/// A heading-sized text node.
pub fn heading(theme: &Theme, content: impl Into<String>) -> impl Bundle {
    text(theme, content, theme.metrics.font_heading, Role::Text)
}

/// A de-emphasized text node.
pub fn muted(theme: &Theme, content: impl Into<String>, size: f32) -> impl Bundle {
    text(theme, content, size, Role::TextMuted)
}

/// A vertical container with panel styling (fill + border + rounded corners).
pub fn panel(theme: &Theme) -> impl Bundle {
    (
        Node {
            flex_direction: FlexDirection::Column,
            padding: UiRect::all(Val::Px(theme.metrics.space_md)),
            row_gap: Val::Px(theme.metrics.space_sm),
            border: UiRect::all(Val::Px(1.0)),
            ..default()
        },
        BackgroundColor(theme.color(Role::Panel)),
        super::theme::ThemedBg(Role::Panel),
        BorderColor::all(theme.color(Role::Border)),
        ThemedBorder(Role::Border),    )
}

/// A horizontal flex row with centered items and the given column gap.
pub fn row(gap: f32) -> Node {
    Node {
        flex_direction: FlexDirection::Row,
        align_items: AlignItems::Center,
        column_gap: Val::Px(gap),
        ..default()
    }
}

/// A vertical flex column with the given row gap.
pub fn column(gap: f32) -> Node {
    Node {
        flex_direction: FlexDirection::Column,
        row_gap: Val::Px(gap),
        ..default()
    }
}

/// A themed button with a centered text label.
///
/// Emits [`bevy_ui_widgets::Activate`] on click; attach an observer on the spawned
/// entity for the action, e.g. `.observe(|_: On<Activate>, ...| { ... })`.
pub fn button(theme: &Theme, label: impl Into<String>) -> impl Bundle {
    (
        Button,
        ThemedButton,
        Interaction::default(),
        Node {
            padding: UiRect::axes(
                Val::Px(theme.metrics.space_md),
                Val::Px(theme.metrics.space_sm),
            ),
            align_items: AlignItems::Center,
            justify_content: JustifyContent::Center,
            border: UiRect::all(Val::Px(1.0)),
            ..default()
        },
        BackgroundColor(theme.color(Role::Surface)),
        BorderColor::all(theme.color(Role::Border)),
        ThemedBorder(Role::Border),        children![text(theme, label, theme.metrics.font_md, Role::Text)],
    )
}
