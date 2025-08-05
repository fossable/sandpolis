use anyhow::Result;
use native_db::ToKey;
use native_model::Model;
use ratatui::{
    crossterm::event::{Event, KeyCode, KeyEventKind},
    text::Text,
    widgets::ListItem,
};
use sandpolis_client::tui::{Panel, selectable_list::ResidentVecWidget};
use sandpolis_core::RealmName;
use sandpolis_database::{DataCreation, DataIdentifier, DataRevision, test_db};
use sandpolis_macros::data;

// Define a simple data structure for our list
#[data]
#[derive(Default)]
pub struct ExampleItem {
    #[secondary_key]
    pub name: String,
    pub description: String,
    pub priority: usize,
    pub completed: bool,
}

#[tokio::main]
async fn main() -> Result<()> {
    // Set up database and resident vector
    let database = test_db!(ExampleItem);
    let db = database.realm(RealmName::default())?;
    let resident_vec = db.resident_vec(())?;

    // Add some sample data
    for i in 1..=5 {
        resident_vec.push(ExampleItem {
            name: format!("Task {}", i),
            description: format!("This is the description for task number {}", i),
            priority: (i % 3) + 1,
            completed: i % 2 == 0,
            _id: DataIdentifier::default(),
            _revision: DataRevision::Latest(0),
            _creation: DataCreation::default(),
        })?;
    }

    // Create the list widget with custom rendering and event handling
    let mut widget = ResidentVecWidget::builder(resident_vec.clone())
        .title("Example Task List")
        .item_renderer(|resident| {
            let data = resident.read();
            let status = if data.completed { "✓" } else { "○" };
            let priority_stars = "★".repeat(data.priority);

            ListItem::new(Text::from(format!(
                "{} {} [{}] {}",
                status, data.name, priority_stars, data.description
            )))
        })
        .event_handler(move |event, list| {
            if let Event::Key(key) = event {
                if key.kind == KeyEventKind::Press {
                    match key.code {
                        // Default navigation
                        KeyCode::Char('j') | KeyCode::Down => {
                            list.select_next();
                            return None;
                        }
                        KeyCode::Char('k') | KeyCode::Up => {
                            list.select_previous();
                            return None;
                        }
                        // Custom actions
                        KeyCode::Enter | KeyCode::Char(' ') => {
                            // Toggle completion status of selected item
                            if let Some(selected) = list.selected() {
                                let _ = selected.update(|data| {
                                    data.completed = !data.completed;
                                    Ok(())
                                });
                            }
                            return None;
                        }
                        KeyCode::Char('a') => {
                            // Add a new task
                            let task_count = list.len() + 1;
                            resident_vec
                                .push(ExampleItem {
                                    name: format!("New Task {}", task_count),
                                    description: format!("Added at runtime - task {}", task_count),
                                    priority: ((task_count - 1) % 3) + 1,
                                    completed: false,
                                    _id: DataIdentifier::default(),
                                    _revision: DataRevision::Latest(0),
                                    _creation: DataCreation::default(),
                                })
                                .unwrap();
                            return None;
                        }
                        KeyCode::Char('d') => {
                            // Delete selected item (not implemented for this example)
                            // You would need to implement removal in ResidentVec
                            return None;
                        }
                        KeyCode::Char('+') => {
                            // Increase priority of selected item
                            if let Some(selected) = list.selected() {
                                let _ = selected.update(|data| {
                                    data.priority = (data.priority + 1).min(5);
                                    Ok(())
                                });
                            }
                            return None;
                        }
                        KeyCode::Char('-') => {
                            // Decrease priority of selected item
                            if let Some(selected) = list.selected() {
                                let _ = selected.update(|data| {
                                    data.priority = (data.priority - 1).max(1);
                                    Ok(())
                                });
                            }
                            return None;
                        }
                        _ => {}
                    }
                }
            }
            // Pass through unhandled events
            Some(event)
        })
        .build()?;

    // Set focus so the widget can handle events
    widget.set_focus(true);

    // Run the TUI
    sandpolis_client::tui::test_widget(widget).await.unwrap();
    Ok(())
}
