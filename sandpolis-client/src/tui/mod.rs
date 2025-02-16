use std::{
    sync::{Arc, RwLock},
    time::Duration,
};

use crate::cli::ClientCommandLine;
use color_eyre::{owo_colors::OwoColorize, Result};
use ratatui::{
    buffer::Buffer,
    crossterm::event::{Event, EventStream, KeyCode, KeyEventKind},
    layout::{Constraint, Layout, Rect},
    style::{Style, Stylize},
    text::Line,
    widgets::{Block, HighlightSpacing, Row, StatefulWidget, Table, TableState, Widget, WidgetRef},
    DefaultTerminal, Frame,
};
use tokio_stream::StreamExt;

pub async fn main(args: ClientCommandLine) -> Result<()> {
    color_eyre::install()?;
    let terminal = ratatui::init();
    let app_result = App {
        fps: args.fps as f32,
        should_quit: false,
    }
    .run(terminal)
    .await;
    ratatui::restore();
    app_result
}

#[derive(Debug, Default)]
struct App {
    fps: f32,
    should_quit: bool,
    #[cfg(feature = "layer-power")]
    power: sandpolis_power::PowerStatusWidget,
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

/// Renders a widget for testing/rapid iteration.
pub async fn test_widget<W>(widget: W) -> Result<()>
where
    W: WidgetRef + Clone, // TODO remove Clone
{
    color_eyre::install()?;
    let mut terminal = ratatui::init();

    let mut should_quit = false;
    let mut interval = tokio::time::interval(Duration::from_secs_f32(1.0 / 30.0));
    let mut events = EventStream::new();

    while !should_quit {
        tokio::select! {
            _ = interval.tick() => { terminal.draw(|frame| frame.render_widget_ref(widget.clone(), frame.area()))?; },
            Some(Ok(event)) = events.next() => {should_quit = true;},
        }
    }
    ratatui::restore();
    Ok(())
}
