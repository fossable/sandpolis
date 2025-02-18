use anyhow::Result;
use ratatui::{
    crossterm::event::{Event, EventStream, KeyCode, KeyEventKind},
    layout::{Constraint, Layout},
    style::Stylize,
    text::Line,
    widgets::WidgetRef,
    DefaultTerminal, Frame,
};
use sandpolis::{config::Configuration, InstanceState};
use std::time::Duration;
use tokio_stream::StreamExt;

pub async fn main(config: Configuration, state: InstanceState) -> Result<()> {
    let terminal = ratatui::init();
    let app_result = App {
        fps: config.client.fps as f32,
        should_quit: false,
        #[cfg(feature = "layer-power")]
        power: todo!(),
    }
    .run(terminal)
    .await;
    ratatui::restore();
    app_result
}

// #[derive(Debug)]
struct App {
    fps: f32,
    should_quit: bool,
    #[cfg(feature = "layer-power")]
    power: sandpolis_power::client::tui::PowerWidget,
}

impl App {
    pub async fn run(mut self, mut terminal: DefaultTerminal) -> Result<()> {
        // self.pull_requests.run();

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

struct ServerListWidget;
struct AgentListWidget;
