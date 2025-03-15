use crate::PowerLayer;
use ratatui::crossterm::event::{Event, KeyCode, KeyEventKind};
use ratatui::text::Text;
use ratatui::widgets::{Block, Borders, Paragraph, Widget, WidgetRef};
use sandpolis_client::tui::EventHandler;
use sandpolis_instance::InstanceId;

// #[derive(Debug)]
pub struct PowerWidget {
    pub instance: InstanceId,
    pub power: PowerLayer,
}

impl WidgetRef for PowerWidget {
    fn render_ref(&self, area: ratatui::layout::Rect, buf: &mut ratatui::buffer::Buffer) {
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

impl EventHandler for PowerWidget {
    fn handle_event(&mut self, event: &Event) {
        if let Event::Key(key) = event {
            if key.kind == KeyEventKind::Press {
                match key.code {
                    KeyCode::Char('j') | KeyCode::Down => {}
                    KeyCode::Char('k') | KeyCode::Up => {}
                    _ => {}
                }
            }
        }
    }
}
