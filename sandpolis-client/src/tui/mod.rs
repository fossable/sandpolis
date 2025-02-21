use color_eyre::Result;
use ratatui::{
    crossterm::event::EventStream,
    widgets::{Widget, WidgetRef},
};
use std::time::Duration;
use tokio_stream::StreamExt;

/// Renders a widget for testing/rapid iteration.
pub async fn test_widget<W>(widget: W) -> Result<()>
where
    W: WidgetRef,
{
    color_eyre::install()?;
    let mut terminal = ratatui::init();

    let mut should_quit = false;
    let mut interval = tokio::time::interval(Duration::from_secs_f32(1.0 / 30.0));
    let mut events = EventStream::new();

    while !should_quit {
        tokio::select! {
            _ = interval.tick() => { terminal.draw(|frame| frame.render_widget(&widget, frame.area()))?; },
            Some(Ok(event)) = events.next() => {should_quit = true;},
        }
    }
    ratatui::restore();
    Ok(())
}
