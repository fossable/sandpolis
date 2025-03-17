use color_eyre::Result;
use ratatui::{
    crossterm::event::Event,
    crossterm::event::EventStream,
    crossterm::event::KeyCode,
    crossterm::event::KeyEventKind,
    widgets::{Widget, WidgetRef},
};
use std::time::Duration;
use tokio::sync::broadcast::{Receiver, Sender};
use tokio_stream::StreamExt;

pub mod help;

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
            _ = interval.tick() => { terminal.draw(|frame| frame.render_widget(&widget, frame.area()))?; },
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
                widget.handle_event(&event); },
        }
    }
    ratatui::restore();
    Ok(())
}

pub trait EventHandler {
    fn handle_event(&mut self, event: &Event);
}

pub trait Panel: WidgetRef + EventHandler {
    fn set_focus(&mut self, focused: bool);
}

#[derive(Clone, Debug)]
pub enum Message {
    FocusChanged,
    ServerSelected,
}

pub type MessageBus<T> = (Sender<T>, Receiver<T>);
