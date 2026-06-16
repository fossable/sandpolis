//! Native theme picker for the Sandpolis GUI.
//!
//! The actual palette/metrics live in [`crate::gui::ui::theme`]; this module is
//! just the `T`-toggled modal that lets the user switch presets at runtime.

use bevy::prelude::*;

use crate::gui::ui::gating::UiPointerState;
use crate::gui::ui::panel::modal_scrim;
use crate::gui::ui::theme::{Role, Theme, ThemePreset, ThemedBg, ThemedBorder};
use crate::gui::ui::widgets::{button, heading, muted, text};

/// Resource to track theme picker UI state.
#[derive(Resource, Default)]
pub struct ThemePickerState {
    pub show: bool,
}

/// Marker for the native theme picker modal root.
#[derive(Component)]
pub struct ThemePickerRoot;

/// Marker for a theme preset row.
#[derive(Component)]
pub struct ThemePresetRow {
    pub preset: ThemePreset,
}

/// Toggle the theme picker with the `T` key (unless a text field is focused).
pub fn handle_theme_picker_toggle(
    ui_pointer: Res<UiPointerState>,
    keyboard: Res<ButtonInput<KeyCode>>,
    mut picker_state: ResMut<ThemePickerState>,
) {
    if ui_pointer.wants_keyboard {
        return;
    }
    if keyboard.just_pressed(KeyCode::KeyT) {
        picker_state.show = !picker_state.show;
    }
}

/// Spawn/despawn the native theme picker modal.
pub fn manage_theme_picker(
    mut commands: Commands,
    theme: Res<Theme>,
    picker_state: Res<ThemePickerState>,
    root: Query<Entity, With<ThemePickerRoot>>,
) {
    let exists = !root.is_empty();
    if picker_state.show && !exists {
        commands
            .spawn((ThemePickerRoot, modal_scrim()))
            .with_children(|scrim| {
                scrim
                    .spawn((
                        Node {
                            flex_direction: FlexDirection::Column,
                            width: Val::Px(340.0),
                            padding: UiRect::all(Val::Px(12.0)),
                            row_gap: Val::Px(8.0),
                            border: UiRect::all(Val::Px(1.0)),
                            ..default()
                        },
                        BackgroundColor(theme.color(Role::Panel)),
                        ThemedBg(Role::Panel),
                        BorderColor::all(theme.color(Role::Border)),
                        ThemedBorder(Role::Border),
                    ))
                    .with_children(|panel| {
                        panel.spawn(heading(&theme, "Theme"));
                        for preset in ThemePreset::all() {
                            let preset = *preset;
                            panel
                                .spawn((
                                    ThemePresetRow { preset },
                                    bevy_ui_widgets::Button,
                                    Interaction::default(),
                                    Node {
                                        flex_direction: FlexDirection::Column,
                                        width: Val::Percent(100.0),
                                        padding: UiRect::axes(Val::Px(8.0), Val::Px(6.0)),
                                        row_gap: Val::Px(2.0),
                                        ..default()
                                    },
                                    BackgroundColor(theme.color(Role::Surface)),
                                    children![
                                        text(&theme, preset.name(), theme.metrics.font_md, Role::Text),
                                        muted(&theme, preset.description(), theme.metrics.font_sm),
                                    ],
                                ))
                                .observe(on_preset_click);
                        }
                        panel.spawn(button(&theme, "Close")).observe(on_theme_close);
                    });
            });
    } else if !picker_state.show && exists {
        for entity in &root {
            commands.entity(entity).despawn();
        }
    }
}

/// Highlight the row for the active preset.
pub fn update_theme_rows(
    theme: Res<Theme>,
    mut rows: Query<(Ref<ThemePresetRow>, &mut BackgroundColor)>,
) {
    let changed = theme.is_changed();
    for (row, mut bg) in &mut rows {
        if changed || row.is_added() {
            bg.0 = if row.preset == theme.preset {
                theme.color(Role::Accent)
            } else {
                theme.color(Role::Surface)
            };
        }
    }
}

fn on_preset_click(
    activate: On<bevy_ui_widgets::Activate>,
    rows: Query<&ThemePresetRow>,
    mut theme: ResMut<Theme>,
) {
    if let Ok(row) = rows.get(activate.entity) {
        theme.set_preset(row.preset);
    }
}

fn on_theme_close(
    _activate: On<bevy_ui_widgets::Activate>,
    mut picker_state: ResMut<ThemePickerState>,
) {
    picker_state.show = false;
}
