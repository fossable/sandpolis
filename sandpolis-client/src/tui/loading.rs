use ratatui::{
    buffer::Buffer,
    layout::{Alignment, Constraint, Layout, Rect},
    prelude::*,
    style::{Style, Stylize},
    text::Line,
    widgets::{Block, Borders, Paragraph, WidgetRef},
};
use tui_popup::SizedWidgetRef;

#[derive(Debug, Clone)]
pub struct LoadingWidget {
    message: String,
    spinner_chars: Vec<char>,
    current_frame: usize,
}

impl LoadingWidget {
    pub fn new(message: &str) -> Self {
        Self {
            message: message.to_string(),
            spinner_chars: vec!['⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏'],
            current_frame: 0,
        }
    }

    pub fn next_frame(&mut self) {
        self.current_frame = (self.current_frame + 1) % self.spinner_chars.len();
    }

    fn get_spinner_char(&self) -> char {
        self.spinner_chars[self.current_frame]
    }
}

impl SizedWidgetRef for LoadingWidget {
    fn width(&self) -> usize {
        40
    }

    fn height(&self) -> usize {
        6
    }
}

impl WidgetRef for LoadingWidget {
    fn render_ref(&self, area: Rect, buf: &mut Buffer) {
        let block = Block::default()
            .title("Please wait")
            .borders(Borders::ALL)
            .style(Style::default().white());

        let inner = block.inner(area);
        block.render(area, buf);

        let chunks = Layout::vertical([
            Constraint::Length(1), // Spacing
            Constraint::Length(1), // Spinner line
            Constraint::Length(1), // Message line
            Constraint::Length(1), // Spacing
        ])
        .split(inner);

        // Render spinner with message
        let spinner_text = format!("{} {}", self.get_spinner_char(), self.message);
        Paragraph::new(Line::from(spinner_text).cyan())
            .alignment(Alignment::Center)
            .render(chunks[1], buf);

        // Render cancel instruction
        Paragraph::new(Line::from("Press Esc to cancel").gray())
            .alignment(Alignment::Center)
            .render(chunks[2], buf);
    }
}
