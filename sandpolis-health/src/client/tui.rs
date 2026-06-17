use super::query_systemd_units;
use crate::systemd::ActiveState;
use ratatui::crossterm::event::Event;
use ratatui::text::{Line, Text};
use ratatui::widgets::{Block, Borders, Paragraph, Widget, WidgetRef};
use sandpolis_client::tui::EventHandler;
use sandpolis_instance::InstanceId;

/// Displays systemd unit health for a single instance.
pub struct HealthWidget {
    pub instance: InstanceId,
}

impl HealthWidget {
    pub fn new(instance: InstanceId) -> Self {
        // Subscribe to live systemd updates for this instance while shown.
        super::subscribe(instance);
        HealthWidget { instance }
    }
}

impl Drop for HealthWidget {
    fn drop(&mut self) {
        super::unsubscribe(self.instance);
    }
}

impl WidgetRef for HealthWidget {
    fn render_ref(&self, area: ratatui::layout::Rect, buf: &mut ratatui::buffer::Buffer) {
        let block = Block::default()
            .title("Service Health")
            .borders(Borders::ALL);

        let units = query_systemd_units(self.instance).unwrap_or_default();

        let mut lines: Vec<Line> = Vec::new();
        if units.is_empty() {
            lines.push(Line::raw("No unit data"));
        } else {
            let failed = units
                .iter()
                .filter(|u| u.active_state == ActiveState::Failed)
                .count();
            lines.push(Line::raw(format!(
                "{} units, {} failed",
                units.len(),
                failed
            )));
            lines.push(Line::raw(""));
            for unit in &units {
                lines.push(Line::raw(format!(
                    "{:<32} {}",
                    unit.name, unit.active_state
                )));
            }
        }

        Paragraph::new(Text::from(lines))
            .block(block)
            .render(area, buf);
    }
}

impl EventHandler for HealthWidget {
    fn handle_event(&mut self, event: Event) -> Option<Event> {
        Some(event)
    }
}
