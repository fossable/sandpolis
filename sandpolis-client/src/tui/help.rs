use ratatui::text::Text;
use ratatui::widgets::Cell;
use ratatui::{buffer::Buffer, crossterm::event::KeyCode, layout::Rect, widgets::WidgetRef};
use ratatui::{
    layout::Constraint,
    style::{Style, Stylize},
    widgets::{Block, Row, Table},
};

/// Shows help message
pub struct HelpWidget {
    pub keybindings: Vec<(KeyCode, String)>,
}

impl HelpWidget {
    pub fn height(&self) -> u16 {
        self.keybindings.len() as u16
    }
}

impl WidgetRef for HelpWidget {
    fn render_ref(&self, area: Rect, buf: &mut Buffer) {
        let rows: Vec<Row> = self
            .keybindings
            .iter()
            .map(|(key, description)| {
                Row::new(vec![
                    Cell::from(description.clone()).italic(),
                    Cell::from(Text::from(key.to_string()).right_aligned()).bold(),
                ])
            })
            .collect();

        let widths = [Constraint::Percentage(90), Constraint::Percentage(10)];
        Table::new(rows, widths)
            .column_spacing(1)
            .style(Style::new().blue())
            .render_ref(area, buf);
    }
}
