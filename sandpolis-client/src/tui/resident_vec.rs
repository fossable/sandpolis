use anyhow::Result;
use ratatui::{
    buffer::Buffer,
    crossterm::event::{Event, KeyCode, KeyEventKind},
    layout::Rect,
    style::{Style, Stylize},
    widgets::{Block, Borders, List, ListItem, ListState, StatefulWidget, WidgetRef},
};
use sandpolis_instance::database::{Data, Resident, ResidentVec, ResidentVecEvent};
use std::sync::{Arc, RwLock};

use super::{EventHandler, Panel};

/// A reusable TUI component for displaying and selecting items from a
/// ResidentVec.
///
/// This component automatically stays in sync with the database through the
/// ResidentVec's built-in listener mechanism. It provides customizable
/// rendering and event handling through a builder pattern.
///
/// # Example
/// ```rust
/// use sandpolis_client::tui::SyncListWidget;
/// use sandpolis_instance::database::ResidentVec;
///
/// let resident_vec: ResidentVec<MyData> = db.resident_vec(())?;
/// let list = SyncListWidget::builder(resident_vec)
///     .title("My List")
///     .item_renderer(|resident| ListItem::new(format!("{}", resident.read().name)))
///     .event_handler(|event, list| {
///         if let Event::Key(key) = event {
///             match key.code {
///                 KeyCode::Enter => {
///                     // Handle selection
///                     return None; // Consume event
///                 }
///                 _ => {}
///             }
///         }
///         // Return Some(event) to continue with default navigation
///         Some(event)
///     })
///     .build()?;
/// ```
pub struct ResidentVecWidget<T>
where
    T: Data + 'static,
{
    state: Arc<RwLock<ResidentVecWidgetState<T>>>,
    title: String,
    focused: bool,
    item_renderer: Box<dyn Fn(&Resident<T>) -> ListItem<'static> + Send + Sync>,
    event_handler: Box<dyn Fn(Event, &ResidentVecWidget<T>) -> Option<Event> + Send + Sync>,
}

struct ResidentVecWidgetState<T>
where
    T: Data,
{
    list_state: ListState,
    items: Vec<Resident<T>>,
}

/// Builder for creating a SyncListWidget with customizable options.
pub struct SyncListWidgetBuilder<T>
where
    T: Data + 'static,
{
    resident_vec: ResidentVec<T>,
    title: Option<String>,
    item_renderer: Option<Box<dyn Fn(&Resident<T>) -> ListItem<'static> + Send + Sync>>,
    event_handler: Option<Box<dyn Fn(Event, &ResidentVecWidget<T>) -> Option<Event> + Send + Sync>>,
}

impl<T> SyncListWidgetBuilder<T>
where
    T: Data + 'static,
{
    fn new(resident_vec: ResidentVec<T>) -> Self {
        Self {
            resident_vec,
            title: None,
            item_renderer: None,
            event_handler: None,
        }
    }

    /// Set the title displayed on the list border.
    pub fn title<S: Into<String>>(mut self, title: S) -> Self {
        self.title = Some(title.into());
        self
    }

    /// Set a custom function to render each item in the list.
    pub fn item_renderer<F>(mut self, renderer: F) -> Self
    where
        F: Fn(&Resident<T>) -> ListItem<'static> + Send + Sync + 'static,
    {
        self.item_renderer = Some(Box::new(renderer));
        self
    }

    /// Set a custom event handler. Return None to consume the event,
    /// Some(event) to continue with default handling.
    pub fn event_handler<F>(mut self, handler: F) -> Self
    where
        F: Fn(Event, &ResidentVecWidget<T>) -> Option<Event> + Send + Sync + 'static,
    {
        self.event_handler = Some(Box::new(handler));
        self
    }

    /// Build the SyncListWidget with the configured options.
    pub fn build(self) -> Result<ResidentVecWidget<T>>
    where
        T: std::fmt::Debug,
    {
        let items: Vec<Resident<T>> = self.resident_vec.iter().collect();

        let mut list_state = ListState::default();
        if !items.is_empty() {
            list_state.select(Some(0));
        }

        let state = Arc::new(RwLock::new(ResidentVecWidgetState { list_state, items }));

        // Register listener for ResidentVec changes
        let state_clone = state.clone();
        self.resident_vec.listen(move |event| {
            let mut state_guard = state_clone.write().unwrap();
            match event {
                ResidentVecEvent::Added(resident) => {
                    state_guard.items.push(resident);

                    // If this is the first item, select it
                    if state_guard.items.len() == 1 {
                        state_guard.list_state.select(Some(0));
                    }
                }
                ResidentVecEvent::Updated(updated_resident) => {
                    // Find and update the corresponding item
                    if let Some(index) = state_guard
                        .items
                        .iter()
                        .position(|item| item.read().id() == updated_resident.read().id())
                    {
                        state_guard.items[index] = updated_resident;
                    }
                }
                ResidentVecEvent::Removed(removed_id) => {
                    // Remove the item from our list
                    let original_len = state_guard.items.len();
                    state_guard
                        .items
                        .retain(|item| item.read().id() != removed_id);

                    // Adjust selection if needed
                    if state_guard.items.len() != original_len {
                        let selected = state_guard.list_state.selected();
                        if state_guard.items.is_empty() {
                            state_guard.list_state.select(None);
                        } else if let Some(selected_index) = selected {
                            let items_len = state_guard.items.len();
                            if selected_index >= items_len {
                                state_guard.list_state.select(Some(items_len - 1));
                            }
                        }
                    }
                }
            }
        });

        let title = self.title.unwrap_or_else(|| "List".to_string());

        let item_renderer = self.item_renderer.unwrap_or_else(|| {
            Box::new(|resident| ListItem::new(format!("{:?}", resident.read())))
        });

        let event_handler = self.event_handler.unwrap_or_else(|| {
            Box::new(|event, list| {
                if let Event::Key(key) = event {
                    if key.kind == KeyEventKind::Press {
                        match key.code {
                            KeyCode::Char('j') | KeyCode::Down => {
                                list.select_next();
                                return None;
                            }
                            KeyCode::Char('k') | KeyCode::Up => {
                                list.select_previous();
                                return None;
                            }
                            _ => {}
                        }
                    }
                }
                Some(event)
            })
        });

        Ok(ResidentVecWidget {
            state,
            title,
            focused: false,
            item_renderer,
            event_handler,
        })
    }
}

impl<T> ResidentVecWidget<T>
where
    T: Data + 'static,
{
    /// Create a builder for configuring a new SyncListWidget.
    pub fn builder(resident_vec: ResidentVec<T>) -> SyncListWidgetBuilder<T> {
        SyncListWidgetBuilder::new(resident_vec)
    }

    /// Get the currently selected item, if any.
    pub fn selected(&self) -> Option<Resident<T>> {
        let state = self.state.read().unwrap();
        state
            .list_state
            .selected()
            .and_then(|index| state.items.get(index))
            .cloned()
    }

    /// Get the number of items in the list.
    pub fn len(&self) -> usize {
        self.state.read().unwrap().items.len()
    }

    /// Check if the list is empty.
    pub fn is_empty(&self) -> bool {
        self.len() == 0
    }

    /// Get all items in the list.
    pub fn items(&self) -> Vec<Resident<T>> {
        self.state.read().unwrap().items.clone()
    }

    /// Select an item by index.
    pub fn select(&self, index: Option<usize>) {
        let mut state = self.state.write().unwrap();
        if let Some(idx) = index {
            if idx < state.items.len() {
                state.list_state.select(Some(idx));
            }
        } else {
            state.list_state.select(None);
        }
    }

    /// Move selection to the next item.
    pub fn select_next(&self) {
        let mut state = self.state.write().unwrap();
        state.list_state.select_next();
    }

    /// Move selection to the previous item.
    pub fn select_previous(&self) {
        let mut state = self.state.write().unwrap();
        state.list_state.select_previous();
    }

    /// Get the index of the currently selected item.
    pub fn selected_index(&self) -> Option<usize> {
        self.state.read().unwrap().list_state.selected()
    }

    /// Render the list with custom styling options.
    pub fn render_with_style(
        &self,
        area: Rect,
        buf: &mut Buffer,
        block_style: Style,
        highlight_style: Style,
    ) {
        let items: Vec<ListItem<'static>> = {
            let state = self.state.read().unwrap();
            let residents = state.items.clone();
            // Drop the lock before processing
            drop(state);

            residents
                .iter()
                .map(|resident| (self.item_renderer)(resident))
                .collect()
        };

        let list = List::new(items)
            .block(
                Block::default()
                    .borders(Borders::ALL)
                    .title(self.title.clone())
                    .style(block_style),
            )
            .highlight_style(highlight_style);

        let mut state = self.state.write().unwrap();
        StatefulWidget::render(list, area, buf, &mut state.list_state);
    }
}

impl<T> WidgetRef for ResidentVecWidget<T>
where
    T: Data + 'static,
{
    fn render_ref(&self, area: Rect, buf: &mut Buffer) {
        let block_style = if self.focused {
            Style::default().white()
        } else {
            Style::default().dim()
        };

        let highlight_style = if self.focused {
            Style::default().white().on_blue()
        } else {
            Style::default().white().on_dark_gray()
        };

        self.render_with_style(area, buf, block_style, highlight_style);
    }
}

impl<T> EventHandler for ResidentVecWidget<T>
where
    T: Data + 'static,
{
    fn handle_event(&mut self, event: Event) -> Option<Event> {
        if !self.focused {
            return Some(event);
        }

        // Call the custom event handler
        (self.event_handler)(event, self)
    }
}

impl<T> Panel for ResidentVecWidget<T>
where
    T: Data + 'static,
{
    fn set_focus(&mut self, focused: bool) {
        self.focused = focused;
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use native_db::ToKey;
    use native_model::Model;
    use ratatui::text::Text;
    use sandpolis_instance::database::{DataCreation, DataIdentifier, DataRevision, test_db};
    use sandpolis_macros::data;
    use sandpolis_instance::realm::RealmName;

    #[data]
    #[derive(Default)]
    pub struct TestItem {
        #[secondary_key]
        pub name: String,
        pub value: i32,
    }

    #[test]
    fn test_sync_list_widget_builder() -> Result<()> {
        let database = test_db!(TestItem);
        let db = database.realm(RealmName::default())?;
        let resident_vec: ResidentVec<TestItem> = db.resident_vec(())?;

        let list = ResidentVecWidget::builder(resident_vec.clone())
            .title("Test List")
            .item_renderer(|resident| {
                let data = resident.read();
                ListItem::new(Text::from(format!("{}: {}", data.name, data.value)))
            })
            .event_handler(|event, list| {
                if let Event::Key(key) = event {
                    if key.kind == KeyEventKind::Press {
                        match key.code {
                            KeyCode::Char('j') | KeyCode::Down => {
                                list.select_next();
                                return None;
                            }
                            KeyCode::Char('k') | KeyCode::Up => {
                                list.select_previous();
                                return None;
                            }
                            KeyCode::Enter => {
                                // Custom behavior for Enter key
                                return None;
                            }
                            _ => {}
                        }
                    }
                }
                Some(event)
            })
            .build()?;

        // Initially empty
        assert_eq!(list.len(), 0);
        assert!(list.is_empty());
        assert!(list.selected().is_none());

        // Test basic builder functionality without database operations
        // that rely on async watchers
        Ok(())
    }

    #[tokio::test]
    async fn test_sync_list_widget_defaults() -> Result<()> {
        let database = test_db!(TestItem);
        let db = database.realm(RealmName::default())?;
        let resident_vec: ResidentVec<TestItem> = db.resident_vec(())?;

        // Test with minimal configuration using defaults
        let mut list = ResidentVecWidget::builder(resident_vec.clone()).build()?;
        list.set_focus(true);

        // Add multiple items
        for i in 0..3 {
            resident_vec.push(TestItem {
                name: format!("Item {}", i + 1),
                value: i + 1,
                ..Default::default()
            })?;
        }

        // Give time for the listener to update
        tokio::time::sleep(tokio::time::Duration::from_millis(10)).await;

        assert_eq!(list.len(), 3);
        assert_eq!(list.selected_index(), Some(0));

        // Test navigation with default event handler
        list.select_next();
        assert_eq!(list.selected_index(), Some(1));

        list.select_next();
        assert_eq!(list.selected_index(), Some(2));

        list.select_previous();
        assert_eq!(list.selected_index(), Some(1));

        Ok(())
    }
}
