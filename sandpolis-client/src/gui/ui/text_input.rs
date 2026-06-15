//! A minimal single-line text input for native `bevy_ui`.
//!
//! `bevy_ui_widgets` ships no text input, so this is a deliberately small v1:
//! click to focus, type / space / backspace, Enter emits [`TextSubmit`], optional
//! password masking. Selection, mid-string cursor movement, IME, and paste are
//! out of scope for now (the cursor is implicitly at the end).
//!
//! Read [`TextInput::value`] directly for "filter as you type" consumers; observe
//! [`TextSubmit`] for Enter-to-confirm.

use super::gating::WantsKeyboard;
use super::theme::{Role, Theme, ThemedBg, ThemedBorder};
use bevy::input::ButtonState;
use bevy::input::keyboard::{Key, KeyboardInput};
use bevy::input_focus::{FocusedInput, InputFocus};
use bevy::picking::Pickable;
use bevy::prelude::*;

/// A single-line text input. The displayed text is a child marked
/// [`TextInputDisplay`].
#[derive(Component)]
pub struct TextInput {
    pub value: String,
    pub mask: bool,
    pub placeholder: String,
}

/// Marker for the child `Text` that renders an input's contents.
#[derive(Component)]
struct TextInputDisplay;

/// Fired (targeted at the input entity) when Enter is pressed while focused.
#[derive(EntityEvent)]
pub struct TextSubmit {
    pub entity: Entity,
}

/// Installs text-input focus/keyboard observers and the display sync system.
pub struct TextInputPlugin;

impl Plugin for TextInputPlugin {
    fn build(&self, app: &mut App) {
        app.add_observer(text_input_focus_on_click)
            .add_observer(text_input_on_key)
            .add_systems(Update, update_text_input_display);
    }
}

/// A themed text input bundle. Spawn it as a child of a container.
pub fn text_input(theme: &Theme, placeholder: impl Into<String>, mask: bool) -> impl Bundle {
    (
        TextInput {
            value: String::new(),
            mask,
            placeholder: placeholder.into(),
        },
        WantsKeyboard,
        Interaction::default(),
        Node {
            min_width: Val::Px(220.0),
            padding: UiRect::axes(Val::Px(8.0), Val::Px(5.0)),
            border: UiRect::all(Val::Px(1.0)),
            align_items: AlignItems::Center,
            ..default()
        },
        BackgroundColor(theme.color(Role::Surface)),
        ThemedBg(Role::Surface),
        BorderColor::all(theme.color(Role::Border)),
        ThemedBorder(Role::Border),
        children![(
            TextInputDisplay,
            // Let clicks fall through to the input box itself.
            Pickable::IGNORE,
            Text::new(String::new()),
            theme.text_font(theme.metrics.font_md),
            TextColor(theme.color(Role::TextMuted)),
        )],
    )
}

/// Focus a text input when it is clicked.
fn text_input_focus_on_click(
    click: On<Pointer<Click>>,
    inputs: Query<(), With<TextInput>>,
    mut focus: ResMut<InputFocus>,
) {
    if inputs.contains(click.entity) {
        focus.0 = Some(click.entity);
    }
}

/// Edit the focused text input in response to keyboard events.
fn text_input_on_key(
    mut event: On<FocusedInput<KeyboardInput>>,
    mut inputs: Query<&mut TextInput>,
    mut commands: Commands,
) {
    let entity = event.focused_entity;
    let Ok(mut input) = inputs.get_mut(entity) else {
        return;
    };
    if event.input.state != ButtonState::Pressed {
        return;
    }
    let mut handled = true;
    match &event.input.logical_key {
        Key::Character(s) => input.value.push_str(s.as_str()),
        Key::Space => input.value.push(' '),
        Key::Backspace => {
            input.value.pop();
        }
        Key::Enter => commands.trigger(TextSubmit { entity }),
        _ => handled = false,
    }
    if handled {
        event.propagate(false);
    }
}

/// Sync each input's display child (masking, placeholder, end cursor) to its value.
fn update_text_input_display(
    theme: Res<Theme>,
    focus: Res<InputFocus>,
    inputs: Query<(Entity, &TextInput, &Children)>,
    mut display: Query<(&mut Text, &mut TextColor), With<TextInputDisplay>>,
) {
    for (entity, input, children) in &inputs {
        let focused = focus.0 == Some(entity);
        let mut shown = if input.value.is_empty() && !focused {
            input.placeholder.clone()
        } else if input.mask {
            "•".repeat(input.value.chars().count())
        } else {
            input.value.clone()
        };
        if focused {
            shown.push('|');
        }
        let muted = input.value.is_empty() && !focused;
        for child in children.iter() {
            if let Ok((mut text, mut color)) = display.get_mut(child) {
                text.0 = shown.clone();
                color.0 = if muted {
                    theme.color(Role::TextMuted)
                } else {
                    theme.color(Role::Text)
                };
            }
        }
    }
}
