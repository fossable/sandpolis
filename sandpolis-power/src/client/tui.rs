use crate::PowerLayer;
use ratatui::text::Text;
use ratatui::widgets::{Block, Borders, Paragraph, Widget};
use sandpolis_instance::InstanceId;

// #[derive(Debug)]
pub struct PowerWidget {
    pub instance: InstanceId,
    pub power: PowerLayer,
}

impl Widget for &PowerWidget {
    fn render(self, area: ratatui::layout::Rect, buf: &mut ratatui::buffer::Buffer) {
        // Create the widget's block
        let block = Block::default()
            .title("System Power Info")
            .borders(Borders::ALL);

        // Render the block
        block.render(area, buf);

        // Render the text inside the block
        let text = vec![
            Text::raw(format!("Power State: {}\n", "state")),
            Text::raw(format!("Uptime: {} seconds\n", "uptime")),
            Text::raw("\n"),
            Text::raw("Controls:"),
            Text::raw("\n- Press 'r' to Restart"),
            Text::raw("\n- Press 's' to Shut Down"),
        ];

        // Display the text inside the block area
        let paragraph = Paragraph::new(Text::raw(format!("Power State: {}\n", "state")))
            .block(Block::default().borders(Borders::NONE));
        paragraph.render(area, buf);
    }
}
