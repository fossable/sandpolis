//! Renders each session's terminal grid into a fixed set of styled text rows.
//!
//! v1 scope: per-run foreground color, bold-as-bright, inverse (fg/bg swap), a
//! full-grid background, and a block cursor. Not yet: per-cell backgrounds
//! (bevy `TextSpan` has no per-span background), underline/italic, selection,
//! and scrollback viewing.

use alacritty_terminal::index::{Column, Line, Point};
use alacritty_terminal::term::cell::Flags;
use alacritty_terminal::term::color::Colors;
use alacritty_terminal::vte::ansi::{Color as AnsiColor, CursorShape, NamedColor, Rgb};
use bevy::prelude::*;
use bevy::text::LineHeight;

use super::{
    CELL_H, CELL_W, FONT_SIZE, ShellStreams, TERM_BG, TerminalFont,
};

/// Marks the cursor overlay node.
#[derive(Component)]
struct TerminalCursor;

/// Rebuild the visible rows of any dirty terminal grid.
pub(super) fn render_terminals(
    mut commands: Commands,
    mut streams: ResMut<ShellStreams>,
    font: Option<Res<TerminalFont>>,
) {
    let Some(font) = font else {
        return;
    };

    for session in streams.sessions.values_mut() {
        if !session.dirty {
            continue;
        }
        let Some(grid) = session.grid else {
            continue;
        };
        // The panel may have been despawned; drop the stale entity reference.
        if commands.get_entity(grid).is_err() {
            session.grid = None;
            continue;
        }

        let cols = session.cols as usize;
        let screen_lines = session.rows as usize;

        // Collect styled runs per line out of the emulator before touching
        // `commands` (the content iterator borrows `term`).
        let content = session.term.renderable_content();
        let colors: &Colors = content.colors;
        let cursor = content.cursor;
        let display_iter = content.display_iter;
        let mut lines: Vec<Vec<Run>> = (0..screen_lines).map(|_| Vec::new()).collect();
        for indexed in display_iter {
            let Point { line: Line(l), .. } = indexed.point;
            if l < 0 || l as usize >= screen_lines {
                continue;
            }
            if indexed
                .cell
                .flags
                .intersects(Flags::WIDE_CHAR_SPACER | Flags::LEADING_WIDE_CHAR_SPACER)
            {
                continue;
            }
            let fg = resolve_fg(indexed.cell.fg, indexed.cell.bg, indexed.cell.flags, colors);
            let hidden = indexed.cell.flags.contains(Flags::HIDDEN);
            let c = if hidden { ' ' } else { indexed.cell.c };
            push_cell(&mut lines[l as usize], c, fg);
        }

        let cursor_shape = cursor.shape;
        let (cur_line, cur_col) = {
            let Point { line: Line(l), column: Column(col) } = cursor.point;
            (l.max(0) as usize, col)
        };

        // Rebuild the grid subtree.
        commands.entity(grid).despawn_related::<Children>();
        commands.entity(grid).with_children(|g| {
            for runs in &lines {
                g.spawn((
                    Text::default(),
                    text_font(&font),
                    LineHeight::Px(CELL_H),
                    TextColor(Color::srgb(0.85, 0.85, 0.85)),
                    Node {
                        height: Val::Px(CELL_H),
                        ..default()
                    },
                ))
                .with_children(|row| {
                    for run in runs {
                        row.spawn((
                            TextSpan::new(run.text.clone()),
                            text_font(&font),
                            TextColor(run.color),
                        ));
                    }
                });
            }

            // Block cursor overlay.
            if cursor_shape != CursorShape::Hidden && cur_line < screen_lines && cur_col < cols {
                g.spawn((
                    TerminalCursor,
                    Node {
                        position_type: PositionType::Absolute,
                        left: Val::Px(cur_col as f32 * CELL_W),
                        top: Val::Px(cur_line as f32 * CELL_H),
                        width: Val::Px(CELL_W),
                        height: Val::Px(CELL_H),
                        ..default()
                    },
                    BackgroundColor(Color::srgba(0.85, 0.85, 0.85, 0.5)),
                ));
            }
        });

        session.dirty = false;
    }
}

/// A run of consecutive cells sharing a foreground color.
struct Run {
    text: String,
    color: Color,
}

fn push_cell(runs: &mut Vec<Run>, c: char, color: Color) {
    match runs.last_mut() {
        Some(run) if run.color == color => run.text.push(c),
        _ => runs.push(Run {
            text: c.to_string(),
            color,
        }),
    }
}

fn text_font(font: &TerminalFont) -> TextFont {
    TextFont {
        font: font.0.clone().into(),
        font_size: FONT_SIZE.into(),
        ..default()
    }
}

/// Resolve the effective foreground color of a cell, honoring inverse video and
/// bold-as-bright.
fn resolve_fg(fg: AnsiColor, bg: AnsiColor, flags: Flags, colors: &Colors) -> Color {
    let (fg, bold) = if flags.contains(Flags::INVERSE) {
        // Render the background color as the visible glyph color.
        (bg, false)
    } else {
        (fg, flags.contains(Flags::BOLD))
    };
    ansi_to_bevy(fg, bold, colors)
}

fn ansi_to_bevy(color: AnsiColor, bold: bool, colors: &Colors) -> Color {
    match color {
        AnsiColor::Spec(rgb) => rgb_to_bevy(rgb),
        AnsiColor::Named(named) => {
            // Bold text uses the bright variant of the 8 base colors.
            let named = if bold {
                match named {
                    NamedColor::Black => NamedColor::BrightBlack,
                    NamedColor::Red => NamedColor::BrightRed,
                    NamedColor::Green => NamedColor::BrightGreen,
                    NamedColor::Yellow => NamedColor::BrightYellow,
                    NamedColor::Blue => NamedColor::BrightBlue,
                    NamedColor::Magenta => NamedColor::BrightMagenta,
                    NamedColor::Cyan => NamedColor::BrightCyan,
                    NamedColor::White => NamedColor::BrightWhite,
                    other => other,
                }
            } else {
                named
            };
            if let Some(rgb) = colors[named] {
                return rgb_to_bevy(rgb);
            }
            match named {
                NamedColor::Foreground => Color::srgb(0.85, 0.85, 0.85),
                NamedColor::Background | NamedColor::Cursor => TERM_BG,
                other => palette_256(other as usize),
            }
        }
        AnsiColor::Indexed(i) => {
            if let Some(rgb) = colors[i as usize] {
                rgb_to_bevy(rgb)
            } else {
                palette_256(i as usize)
            }
        }
    }
}

fn rgb_to_bevy(rgb: Rgb) -> Color {
    Color::srgb_u8(rgb.r, rgb.g, rgb.b)
}

/// Static xterm 256-color palette.
fn palette_256(i: usize) -> Color {
    const BASE: [(u8, u8, u8); 16] = [
        (0x00, 0x00, 0x00),
        (0xcd, 0x00, 0x00),
        (0x00, 0xcd, 0x00),
        (0xcd, 0xcd, 0x00),
        (0x00, 0x00, 0xee),
        (0xcd, 0x00, 0xcd),
        (0x00, 0xcd, 0xcd),
        (0xe5, 0xe5, 0xe5),
        (0x7f, 0x7f, 0x7f),
        (0xff, 0x00, 0x00),
        (0x00, 0xff, 0x00),
        (0xff, 0xff, 0x00),
        (0x5c, 0x5c, 0xff),
        (0xff, 0x00, 0xff),
        (0x00, 0xff, 0xff),
        (0xff, 0xff, 0xff),
    ];
    if i < 16 {
        let (r, g, b) = BASE[i];
        Color::srgb_u8(r, g, b)
    } else if i < 232 {
        // 6x6x6 color cube.
        let i = i - 16;
        let step = |v: usize| if v == 0 { 0 } else { (v * 40 + 55) as u8 };
        Color::srgb_u8(step(i / 36 % 6), step(i / 6 % 6), step(i % 6))
    } else if i < 256 {
        // Grayscale ramp.
        let v = (8 + (i - 232) * 10) as u8;
        Color::srgb_u8(v, v, v)
    } else {
        Color::srgb(0.85, 0.85, 0.85)
    }
}
