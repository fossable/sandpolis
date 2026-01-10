use anyhow::Result;
use ratatui::{
    buffer::Buffer,
    crossterm::event::{Event, KeyCode, KeyEventKind},
    layout::{Alignment, Constraint, Layout, Rect},
    style::{Style, Stylize},
    text::Line,
    widgets::{Block, Borders, ListItem, Paragraph, Widget, WidgetRef},
};
use sandpolis_client::tui::{EventHandler, Panel, help::HelpWidget};
use std::sync::{Arc, RwLock};

pub struct AgentListWidget {
    state: Arc<RwLock<AgentListWidgetState>>,
    focused: bool,
}

struct AgentListWidgetState {
    mode: AgentListWidgetMode,
    help_widget: HelpWidget,
}

enum AgentListWidgetMode {
    Normal,
    Selecting,
}

impl AgentListWidget {
    pub fn new() -> Result<Self> {
        let state = Arc::new(RwLock::new(AgentListWidgetState {
            mode: AgentListWidgetMode::Normal,
            help_widget: HelpWidget::new(vec![
                (KeyCode::Char('j'), "Move down".to_string()),
                (KeyCode::Char('k'), "Move up".to_string()),
                (KeyCode::Right, "Connect to agent".to_string()),
            ]),
        }));

        Ok(Self {
            state,
            focused: true,
        })
    }
}

impl WidgetRef for AgentListWidget {
    fn render_ref(&self, area: Rect, buf: &mut Buffer) {
        let state = self.state.read().unwrap();

        let block = Block::default().borders(Borders::ALL).title("Agent List");

        let [list_area, help_area] = Layout::vertical([
            Constraint::Min(1),
            Constraint::Length(state.help_widget.height()),
        ])
        .areas(block.inner(area));

        block.render(area, buf);

        // Render empty state message
        let empty_message = vec![
            Line::from(""),
            Line::from("No agents connected").gray(),
            Line::from(""),
            Line::from("Agents will appear here when they connect to the server").cyan(),
        ];

        Paragraph::new(empty_message)
            .alignment(Alignment::Center)
            .render(list_area, buf);

        // Render help
        if self.focused {
            state.help_widget.render_ref(help_area, buf);
        }
    }
}

impl EventHandler for AgentListWidget {
    fn handle_event(&mut self, event: Event) -> Option<Event> {
        let mut state = self.state.write().unwrap();

        if let Event::Key(key) = event {
            if key.kind == KeyEventKind::Press {
                match state.mode {
                    AgentListWidgetMode::Normal => {
                        state.help_widget.handle_event(event.clone())?;

                        match key.code {
                            KeyCode::Char('j') | KeyCode::Down => {
                                // TODO: Navigate down when agents exist
                                return None;
                            }
                            KeyCode::Char('k') | KeyCode::Up => {
                                // TODO: Navigate up when agents exist
                                return None;
                            }
                            KeyCode::Right => {
                                // TODO: Connect to selected agent
                                return None;
                            }
                            _ => {}
                        }
                    }
                    AgentListWidgetMode::Selecting => {
                        // TODO: Handle selection mode
                    }
                }
            }
        }

        Some(event)
    }
}

impl Panel for AgentListWidget {
    fn set_focus(&mut self, focused: bool) {
        self.focused = focused;
    }
}
