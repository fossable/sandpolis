use color_eyre::Result;
use crossterm::event::{Event, EventStream, KeyCode, KeyEventKind, KeyModifiers};
use ratatui::buffer::Buffer;
use ratatui::layout::{Alignment, Rect};
use ratatui::widgets::{Block, Borders, Paragraph, Widget, WidgetRef, Wrap};
use std::time::Duration;
use tokio_stream::StreamExt;

pub mod help;
pub mod loading;
pub mod resident_vec;

/// Run a full-screen TUI driven by a single root widget until the user quits
/// (`q` or Ctrl-C). Sets up and tears down the terminal. Each client subcommand
/// builds its own root and calls this.
pub async fn run_tui<W>(fps: f32, mut root: W) -> anyhow::Result<()>
where
    W: WidgetRef + EventHandler,
{
    let mut terminal = ratatui::init();
    let mut should_quit = false;
    let period = Duration::from_secs_f32(1.0 / fps.max(1.0));
    let mut interval = tokio::time::interval(period);
    let mut events = EventStream::new();

    while !should_quit {
        tokio::select! {
            _ = interval.tick() => {
                terminal.draw(|frame| root.render_ref(frame.area(), frame.buffer_mut()))?;
            }
            Some(Ok(event)) = events.next() => {
                // The root handler gets first crack; anything it doesn't consume
                // can quit the app.
                if let Some(Event::Key(key)) = root.handle_event(event) {
                    if key.kind == KeyEventKind::Press {
                        match key.code {
                            KeyCode::Char('q') => should_quit = true,
                            KeyCode::Char('c') if key.modifiers.contains(KeyModifiers::CONTROL) => {
                                should_quit = true;
                            }
                            _ => {}
                        }
                    }
                }
            }
        }
    }
    ratatui::restore();
    Ok(())
}

/// A minimal full-screen panel for not-yet-implemented subcommands. Renders a
/// title and a hint, and passes all events through so `run_tui` can quit.
pub struct PlaceholderPanel {
    title: String,
}

impl PlaceholderPanel {
    pub fn new(title: impl Into<String>) -> Self {
        Self {
            title: title.into(),
        }
    }
}

impl WidgetRef for PlaceholderPanel {
    fn render_ref(&self, area: Rect, buf: &mut Buffer) {
        let block = Block::default().title(self.title.clone()).borders(Borders::ALL);
        let inner = block.inner(area);
        block.render(area, buf);
        Paragraph::new("Not yet implemented.\n\nPress q to quit.")
            .alignment(Alignment::Center)
            .wrap(Wrap { trim: true })
            .render(inner, buf);
    }
}

impl EventHandler for PlaceholderPanel {
    fn handle_event(&mut self, event: Event) -> Option<Event> {
        Some(event)
    }
}

/// Renders a widget for testing/rapid iteration.
pub async fn test_widget<W>(mut widget: W) -> Result<()>
where
    W: WidgetRef + EventHandler,
{
    color_eyre::install()?;
    let mut terminal = ratatui::init();

    let mut should_quit = false;
    let mut interval = tokio::time::interval(Duration::from_secs_f32(1.0 / 30.0));
    let mut events = EventStream::new();

    while !should_quit {
        tokio::select! {
            _ = interval.tick() => { terminal.draw(|frame| widget.render_ref(frame.area(), frame.buffer_mut()))?; },
            Some(Ok(event)) = events.next() => {
                if let Event::Key(key) = event {
                    if key.kind == KeyEventKind::Press {
                        match key.code {
                            KeyCode::Esc => {
                                should_quit = true;
                            }
                            _ => {}
                        }
                    }
                }
                widget.handle_event(event); },
        }
    }
    ratatui::restore();
    Ok(())
}

pub trait EventHandler {
    fn handle_event(&mut self, event: Event) -> Option<Event>;
}

pub trait Panel: WidgetRef + EventHandler {
    fn set_focus(&mut self, focused: bool);
}
