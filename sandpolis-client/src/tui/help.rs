use ratatui::{buffer::Buffer, crossterm::event::KeyCode, layout::Rect};
use std::collections::HashMap;

/// Shows help message
pub struct HelpWidget {
    pub keybindings: HashMap<KeyCode, String>,
}

impl HelpWidget {
    pub fn height(&self) -> u16 {
        todo!()
    }
}

impl WidgetRef for HelpWidget {
    fn render_ref(&self, area: Rect, buf: &mut Buffer) {
        let block = Block::default().borders(Borders::ALL);
        block.render(area, buf);
    }
}
