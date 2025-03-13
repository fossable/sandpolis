use crate::{InstanceState, config::Configuration};
use anyhow::Result;
use ratatui::{
    DefaultTerminal, Frame,
    buffer::Buffer,
    crossterm::event::{Event, EventStream, KeyCode, KeyEventKind},
    layout::{Constraint, Layout, Rect},
    style::Stylize,
    text::Line,
    widgets::WidgetRef,
};
use ratatui_image::picker::Picker;
use sandpolis_client::tui::{Message, Panel};
use server_list::ServerListWidget;
use std::time::Duration;
use tokio::sync::broadcast::{self, Receiver, Sender};
use tokio_stream::StreamExt;
use tracing::debug;

pub mod server_list;

// #[derive(Debug)]
struct App {
    graphics: Option<Picker>,
    fps: f32,
    should_quit: bool,
    bus: (Sender<Message>, Receiver<Message>),
    panels: PanelContainer,
    // Layers
    // #[cfg(feature = "layer-power")]
    // power: sandpolis_power::client::tui::PowerWidget,
}

pub async fn main(config: Configuration, state: InstanceState) -> Result<()> {
    let terminal = ratatui::init();
    let app_result = App {
        // Query terminal graphics capabilities
        graphics: Picker::from_query_stdio().ok(),
        fps: config.client.fps as f32,
        should_quit: false,
        panels: PanelContainer {
            panels: vec![Box::new(ServerListWidget::new(state.server.clone()))],
            focused: 0,
            left: 0,
        },
        bus: broadcast::channel(16),
        // #[cfg(feature = "layer-power")]
        // power: todo!(),
    }
    .run(terminal)
    .await;
    ratatui::restore();
    app_result
}

impl App {
    pub async fn run(mut self, mut terminal: DefaultTerminal) -> Result<()> {
        // TODO log output needs to stop going to stderr
        if let Some(picker) = self.graphics.as_ref() {
            debug!(terminal_info = ?picker, "Detected terminal graphics capabilties");
        } else {
            debug!("No terminal graphics capabilties detected");
        }

        let period = Duration::from_secs_f32(1.0 / self.fps);
        let mut interval = tokio::time::interval(period);
        let mut events = EventStream::new();

        while !self.should_quit {
            tokio::select! {
                _ = interval.tick() => { terminal.draw(|frame| frame.render_widget_ref(&self.panels, frame.area()))?; },
                Some(Ok(event)) = events.next() => self.handle_event(&event),
            }
        }
        Ok(())
    }

    fn handle_event(&mut self, event: &Event) {
        if let Event::Key(key) = event {
            if key.kind == KeyEventKind::Press {
                match key.code {
                    KeyCode::Char('q') | KeyCode::Esc => self.should_quit = true,
                    // KeyCode::Char('j') | KeyCode::Down => self.pull_requests.scroll_down(),
                    // KeyCode::Char('k') | KeyCode::Up => self.pull_requests.scroll_up(),
                    _ => {}
                }
            }
        }
    }
}

struct PanelContainer {
    panels: Vec<Box<dyn Panel>>,
    /// Index of focused panel
    focused: usize,
    /// Index of leftmost panel
    left: usize,
}

impl WidgetRef for &PanelContainer {
    fn render_ref(&self, area: Rect, buf: &mut Buffer) {
        let [left, right] =
            Layout::horizontal([Constraint::Percentage(38), Constraint::Percentage(62)])
                .areas(area);

        self.panels
            .get(self.left)
            .expect("there's always a left panel")
            .render_ref(left, buf);
    }
}
