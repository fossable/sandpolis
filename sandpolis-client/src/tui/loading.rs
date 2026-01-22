use ratatui::{
    buffer::Buffer,
    layout::{Alignment, Constraint, Layout, Rect},
    style::{Style, Stylize},
    text::Line,
    widgets::{Block, Borders, Paragraph, Widget, WidgetRef},
};
use std::time::{Duration, Instant};
use tui_popup::KnownSize;

#[derive(Debug, Clone)]
pub struct LoadingWidget {
    message: String,
    spinner_chars: Vec<char>,
    current_frame: usize,
    last_update: Instant,
    frame_duration: Duration,
}

impl LoadingWidget {
    pub fn new(message: &str) -> Self {
        Self {
            message: message.to_string(),
            spinner_chars: vec!['⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏'],
            current_frame: 0,
            last_update: Instant::now(),
            frame_duration: Duration::from_millis(100), // 10 FPS animation
        }
    }

    pub fn next_frame(&mut self) {
        self.current_frame = (self.current_frame + 1) % self.spinner_chars.len();
        self.last_update = Instant::now();
    }

    pub fn update_animation(&mut self) {
        let now = Instant::now();
        if now.duration_since(self.last_update) >= self.frame_duration {
            self.next_frame();
        }
    }

    fn get_spinner_char(&self) -> char {
        // Calculate current frame based on elapsed time for automatic animation
        let elapsed = Instant::now().duration_since(self.last_update);
        let additional_frames = (elapsed.as_millis() / self.frame_duration.as_millis()) as usize;
        let current_frame = (self.current_frame + additional_frames) % self.spinner_chars.len();
        self.spinner_chars[current_frame]
    }
}

impl KnownSize for LoadingWidget {
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
