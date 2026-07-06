//! Translates keyboard input into terminal input bytes while a terminal grid is
//! focused, and sends them to the agent as `Stdin`.

use alacritty_terminal::term::TermMode;
use bevy::input::ButtonState;
use bevy::input::keyboard::{Key, KeyboardInput};
use bevy::input_focus::InputFocus;
use bevy::prelude::*;

use super::{ShellStreams, TerminalGrid};
use crate::session::ShellSessionStreamRequest;

pub(super) fn terminal_keyboard_input(
    mut events: MessageReader<KeyboardInput>,
    keyboard: Res<ButtonInput<KeyCode>>,
    input_focus: Res<InputFocus>,
    grids: Query<&TerminalGrid>,
    streams: ResMut<ShellStreams>,
) {
    // Only act when a terminal grid holds focus.
    let Some(focused) = input_focus.get() else {
        events.clear();
        return;
    };
    let Ok(grid) = grids.get(focused) else {
        events.clear();
        return;
    };
    let streams = streams.into_inner();
    let Some(session) = streams.sessions.get(&grid.instance) else {
        events.clear();
        return;
    };

    let ctrl = keyboard.any_pressed([KeyCode::ControlLeft, KeyCode::ControlRight]);
    let alt = keyboard.any_pressed([KeyCode::AltLeft, KeyCode::AltRight]);
    let shift = keyboard.any_pressed([KeyCode::ShiftLeft, KeyCode::ShiftRight]);
    let app_cursor = session.term.mode().contains(TermMode::APP_CURSOR);

    let mut out = Vec::new();
    for event in events.read() {
        if event.state != ButtonState::Pressed {
            continue;
        }
        if let Some(bytes) = encode(&event.logical_key, ctrl, alt, shift, app_cursor) {
            out.extend_from_slice(&bytes);
        }
    }

    if !out.is_empty() {
        let _ = session
            .outbound
            .try_send(ShellSessionStreamRequest::Stdin { data: out });
    }
}

/// SS3/CSI cursor sequence for the arrow/home/end keys, honoring DECCKM.
fn cursor_seq(app_cursor: bool, final_byte: u8) -> Vec<u8> {
    let intro: &[u8] = if app_cursor { b"\x1bO" } else { b"\x1b[" };
    let mut v = intro.to_vec();
    v.push(final_byte);
    v
}

fn encode(key: &Key, ctrl: bool, alt: bool, shift: bool, app_cursor: bool) -> Option<Vec<u8>> {
    let bytes = match key {
        Key::Character(s) => {
            let mut out = Vec::new();
            if ctrl {
                // Ctrl+<letter> and a few punctuation controls.
                if let Some(c) = s.chars().next() {
                    let upper = c.to_ascii_uppercase();
                    if upper.is_ascii_alphabetic() {
                        out.push((upper as u8) & 0x1f);
                    } else {
                        match c {
                            ' ' => out.push(0x00),
                            '[' => out.push(0x1b),
                            '\\' => out.push(0x1c),
                            ']' => out.push(0x1d),
                            '^' => out.push(0x1e),
                            '_' => out.push(0x1f),
                            _ => out.extend_from_slice(s.as_bytes()),
                        }
                    }
                }
            } else {
                out.extend_from_slice(s.as_bytes());
            }
            if alt {
                let mut prefixed = vec![0x1b];
                prefixed.append(&mut out);
                out = prefixed;
            }
            out
        }
        Key::Enter => vec![b'\r'],
        Key::Backspace => vec![0x7f],
        Key::Tab if shift => b"\x1b[Z".to_vec(),
        Key::Tab => vec![b'\t'],
        Key::Escape => vec![0x1b],
        Key::Space if ctrl => vec![0x00],
        Key::Space => vec![b' '],
        Key::ArrowUp => cursor_seq(app_cursor, b'A'),
        Key::ArrowDown => cursor_seq(app_cursor, b'B'),
        Key::ArrowRight => cursor_seq(app_cursor, b'C'),
        Key::ArrowLeft => cursor_seq(app_cursor, b'D'),
        Key::Home => cursor_seq(app_cursor, b'H'),
        Key::End => cursor_seq(app_cursor, b'F'),
        Key::PageUp => b"\x1b[5~".to_vec(),
        Key::PageDown => b"\x1b[6~".to_vec(),
        Key::Delete => b"\x1b[3~".to_vec(),
        Key::Insert => b"\x1b[2~".to_vec(),
        Key::F1 => b"\x1bOP".to_vec(),
        Key::F2 => b"\x1bOQ".to_vec(),
        Key::F3 => b"\x1bOR".to_vec(),
        Key::F4 => b"\x1bOS".to_vec(),
        Key::F5 => b"\x1b[15~".to_vec(),
        Key::F6 => b"\x1b[17~".to_vec(),
        Key::F7 => b"\x1b[18~".to_vec(),
        Key::F8 => b"\x1b[19~".to_vec(),
        Key::F9 => b"\x1b[20~".to_vec(),
        Key::F10 => b"\x1b[21~".to_vec(),
        Key::F11 => b"\x1b[23~".to_vec(),
        Key::F12 => b"\x1b[24~".to_vec(),
        _ => return None,
    };
    if bytes.is_empty() { None } else { Some(bytes) }
}
