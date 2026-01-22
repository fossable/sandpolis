use crate::tui::EventHandler;
use crossterm::event::{Event, KeyCode, KeyEventKind};
use ratatui::{
    buffer::Buffer,
    layout::{Constraint, Layout, Rect},
    style::{Color, Style, Stylize},
    text::{Line, Span, Text},
    widgets::{Block, Borders, Clear, Paragraph, WidgetRef},
};
use std::time::{Duration, Instant};

/// Shows help message with key bindings
pub struct HelpWidget {
    pub keybindings: Vec<(KeyCode, String, bool)>, // KeyCode, Description, Enabled
    highlighted_key: Option<(KeyCode, Instant)>,
    highlight_duration: Duration,
}

impl HelpWidget {
    pub fn new(keybindings: Vec<(KeyCode, String)>) -> Self {
        // Convert to enabled keybindings by default
        let enabled_keybindings = keybindings
            .into_iter()
            .map(|(key, desc)| (key, desc, true))
            .collect();

        Self {
            keybindings: enabled_keybindings,
            highlighted_key: None,
            highlight_duration: Duration::from_millis(300),
        }
    }

    pub fn new_with_states(keybindings: Vec<(KeyCode, String, bool)>) -> Self {
        Self {
            keybindings,
            highlighted_key: None,
            highlight_duration: Duration::from_millis(300),
        }
    }

    pub fn set_enabled(&mut self, key: KeyCode, enabled: bool) {
        for (binding_key, _, is_enabled) in &mut self.keybindings {
            if binding_key == &key {
                *is_enabled = enabled;
                break;
            }
        }
    }

    pub fn highlight_key(&mut self, key: KeyCode) {
        self.highlighted_key = Some((key, Instant::now()));
    }

    fn is_key_highlighted(&self, key: &KeyCode) -> bool {
        if let Some((highlighted_key, timestamp)) = &self.highlighted_key {
            if highlighted_key == key {
                return timestamp.elapsed() < self.highlight_duration;
            }
        }
        false
    }

    pub fn height(&self) -> u16 {
        // Return a conservative estimate that allows for wrapping
        // Since we don't know the width yet, be very generous to ensure wrapping works
        if self.keybindings.is_empty() {
            0
        } else {
            // Worst case: assume very narrow terminal, so each keybinding might need its
            // own line This ensures we always get enough space
            (self.keybindings.len() as u16).min(3).max(1) // Cap at 3 lines but
            // ensure at least 1
        }
    }

    pub fn height_for_width(&self, width: u16) -> u16 {
        self.calculate_height(width)
    }

    fn calculate_height(&self, available_width: u16) -> u16 {
        if self.keybindings.is_empty() {
            return 0;
        }

        let mut current_line_width = 0u16;
        let mut lines = 1u16;

        for (i, (key, description, _enabled)) in self.keybindings.iter().enumerate() {
            let separator_width = if i > 0 && current_line_width > 0 {
                5
            } else {
                0
            }; // "  •  "
            let key_width = self.format_key(key).len() as u16 + 2; // [key]
            let desc_width = description.len() as u16 + 1; // description + space
            let item_width = separator_width + key_width + desc_width;

            if current_line_width + item_width > available_width && current_line_width > 0 {
                lines += 1;
                current_line_width = key_width + desc_width + 1; // Reset without separator
            } else {
                current_line_width += item_width;
            }
        }

        lines
    }

    fn format_key(&self, key: &KeyCode) -> String {
        match key {
            KeyCode::Char(c) => c.to_string(),
            KeyCode::Enter => "Enter".to_string(),
            KeyCode::Esc => "Esc".to_string(),
            KeyCode::Tab => "Tab".to_string(),
            KeyCode::BackTab => "Shift+Tab".to_string(),
            KeyCode::Backspace => "Backspace".to_string(),
            KeyCode::Delete => "Delete".to_string(),
            KeyCode::Left => "←".to_string(),
            KeyCode::Right => "→".to_string(),
            KeyCode::Up => "↑".to_string(),
            KeyCode::Down => "↓".to_string(),
            KeyCode::Home => "Home".to_string(),
            KeyCode::End => "End".to_string(),
            KeyCode::PageUp => "PgUp".to_string(),
            KeyCode::PageDown => "PgDn".to_string(),
            KeyCode::F(n) => format!("F{}", n),
            _ => format!("{:?}", key),
        }
    }
}

impl WidgetRef for HelpWidget {
    fn render_ref(&self, area: Rect, buf: &mut Buffer) {
        if self.keybindings.is_empty() {
            return;
        }

        let mut lines = Vec::new();
        let mut current_line_spans = Vec::new();
        let mut current_line_width = 0u16;

        for (key, description, enabled) in self.keybindings.iter() {
            let separator_width = if !current_line_spans.is_empty() { 5 } else { 0 }; // "  •  "
            let key_width = self.format_key(key).len() as u16 + 2; // [key]
            let desc_width = description.len() as u16 + 1; // description + space
            let item_width = separator_width + key_width + desc_width;

            // Check if we need to wrap to a new line
            if current_line_width + item_width > area.width && !current_line_spans.is_empty() {
                // Finish current line and start a new one
                lines.push(Line::from(current_line_spans));
                current_line_spans = Vec::new();
                current_line_width = 0;
            }

            // Add separator if not the first item on the line
            if !current_line_spans.is_empty() {
                current_line_spans
                    .push(Span::styled("  •  ", Style::default().fg(Color::DarkGray)));
                current_line_width += 5;
            }

            // Key in a box-like format with different styling based on state
            let key_style = if !enabled {
                // Disabled keys are greyed out
                Style::default().fg(Color::DarkGray)
            } else if self.is_key_highlighted(key) {
                // Highlighted keys (recently pressed)
                Style::default().fg(Color::Yellow).bg(Color::Blue).bold()
            } else {
                // Normal enabled keys
                Style::default().fg(Color::Cyan).bold()
            };

            current_line_spans.push(Span::styled(
                format!("[{}]", self.format_key(key)),
                key_style,
            ));
            current_line_width += key_width;

            current_line_spans.push(Span::styled(" ", Style::default()));
            current_line_width += 1;

            // Description with appropriate styling
            let desc_style = if !enabled {
                Style::default().fg(Color::DarkGray)
            } else {
                Style::default().fg(Color::Gray)
            };

            current_line_spans.push(Span::styled(description.clone(), desc_style));
            current_line_width += description.len() as u16;
        }

        // Add the final line if there are remaining spans
        if !current_line_spans.is_empty() {
            lines.push(Line::from(current_line_spans));
        }

        let help_text = Text::from(lines);

        ratatui::widgets::Widget::render(
            Paragraph::new(help_text).style(Style::default().bg(Color::Black)),
            area,
            buf,
        );
    }
}

impl EventHandler for HelpWidget {
    fn handle_event(&mut self, event: Event) -> Option<Event> {
        if let Event::Key(key) = &event {
            if key.kind == KeyEventKind::Press {
                // Check if this key is in our keybindings and highlight it (only if enabled)
                for (keybinding, _, enabled) in &self.keybindings {
                    if keybinding == &key.code && *enabled {
                        self.highlight_key(key.code);
                        break;
                    }
                }
            }
        }
        // Never consume events - always pass them through
        Some(event)
    }
}
