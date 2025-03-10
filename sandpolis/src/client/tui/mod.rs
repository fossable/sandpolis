use crate::{InstanceState, config::Configuration};
use anyhow::Result;
use ratatui::{
    DefaultTerminal, Frame,
    crossterm::event::{Event, EventStream, KeyCode, KeyEventKind},
    layout::{Constraint, Layout},
    style::Stylize,
    text::Line,
    widgets::WidgetRef,
};
use ratatui_image::picker::Picker;
use sandpolis_client::tui::Message;
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
    server_list: ServerListWidget,
    bus: (Sender<Message>, Receiver<Message>),
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
        server_list: ServerListWidget::new(state.server.clone()),
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
                _ = interval.tick() => { terminal.draw(|frame| self.draw(frame))?; },
                Some(Ok(event)) = events.next() => self.handle_event(&event),
            }
        }
        Ok(())
    }

    fn draw(&self, frame: &mut Frame) {
        let vertical = Layout::vertical([Constraint::Length(1), Constraint::Fill(1)]);
        let [title_area, body_area] = vertical.areas(frame.area());
        let title = Line::from("Ratatui async example").centered().bold();
        frame.render_widget(title, title_area);
        // frame.render_widget(&self.pull_requests, body_area);
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

struct AgentListWidget;
