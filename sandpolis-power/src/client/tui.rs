use ratatui::{
    backend::CrosstermBackend,
    layout::{Constraint, Direction, Layout},
    widgets::{Block, Borders, Paragraph, Text, Widget},
    Terminal,
};

// Define a custom widget
pub struct PowerStatusWidget {
    power_state: String,
    uptime: u64,
}

impl PowerStatusWidget {
    pub fn new(power_state: String, uptime: u64) -> Self {
        PowerStatusWidget {
            power_state,
            uptime,
        }
    }
}

impl Widget for PowerStatusWidget {
    fn render(self, area: ratatui::layout::Rect, buf: &mut ratatui::buffer::Buffer) {
        // Create the widget's block
        let block = Block::default()
            .title("System Power Info")
            .borders(Borders::ALL);

        // Render the block
        block.render(area, buf);

        // Render the text inside the block
        let text = vec![
            Text::raw(format!("Power State: {}\n", self.power_state)),
            Text::raw(format!("Uptime: {} seconds\n", self.uptime)),
            Text::raw("\n"),
            Text::raw("Controls:"),
            Text::raw("\n- Press 'r' to Restart"),
            Text::raw("\n- Press 's' to Shut Down"),
        ];

        // Display the text inside the block area
        let paragraph = Paragraph::new(text).block(Block::default().borders(Borders::NONE));
        paragraph.render(area, buf);
    }
}
