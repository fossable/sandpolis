//! Themed wrapper around Bevy's native [`EditableText`] widget.
//!
//! This is intentionally thin: it only applies the app [`Theme`] (border,
//! background, font, cursor colors) to a native [`EditableText`] node. Cursor
//! movement, text selection, IME, and clipboard paste are all handled upstream by
//! `bevy_ui_widgets::EditableTextInputPlugin` (installed via `UiWidgetsPlugins`).
//!
//! Features the native widget doesn't yet provide (placeholder text, password
//! masking, submit events) are deliberately omitted — we'll adopt them from
//! upstream when they land rather than maintaining our own versions.
//!
//! Read the current contents by querying [`EditableText`] and calling
//! [`EditableText::value`]; the currently focused entity is in [`InputFocus`].
//! Because focus targets the editor entity directly (and `FocusedInput` bubbles
//! *up*), attach the caller's marker component to the same entity as the editor,
//! e.g. `parent.spawn((MyInput, text_input(theme)))`.

use super::gating::WantsKeyboard;
use super::theme::{Role, Theme, ThemedBg, ThemedBorder, ThemedText};
use bevy::input_focus::tab_navigation::TabIndex;
use bevy::prelude::*;
use bevy::text::{EditableText, TextCursorStyle};

/// A themed single-line [`EditableText`] bundle.
pub fn text_input(theme: &Theme) -> impl Bundle {
    let font_size = theme.metrics.font_md;
    (
        // Lets the input gating ([`UiPointerState::wants_keyboard`]) suppress world
        // hotkeys while this field is focused.
        WantsKeyboard,
        EditableText {
            allow_newlines: false,
            ..default()
        },
        TextLayout::no_wrap(),
        TextCursorStyle {
            color: theme.color(Role::Text),
            selection_color: theme.color(Role::Accent),
            ..default()
        },
        theme.text_font(font_size),
        TextColor(theme.color(Role::Text)),
        ThemedText(Role::Text),
        TabIndex(0),
        Node {
            min_width: Val::Px(220.0),
            padding: UiRect::axes(Val::Px(8.0), Val::Px(5.0)),
            border: UiRect::all(Val::Px(1.0)),
            align_items: AlignItems::Center,
            overflow: Overflow::clip(),
            // Don't let a constrained/scrolling parent squash the field.
            flex_shrink: 0.0,
            ..default()
        },
        BackgroundColor(theme.color(Role::Surface)),
        ThemedBg(Role::Surface),
        BorderColor::all(theme.color(Role::Border)),
        ThemedBorder(Role::Border),
    )
}
