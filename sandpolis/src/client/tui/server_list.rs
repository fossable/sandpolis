// TODO rename server_selection
use ratatui::{
    buffer::Buffer,
    crossterm::event::{Event, KeyCode, KeyEventKind},
    layout::{Constraint, Direction, Layout, Rect},
    style::{Style, Stylize},
    text::{Line, Text},
    widgets::{
        Block, Borders, List, ListItem, ListState, StatefulWidget, StatefulWidgetRef, Widget,
        WidgetRef,
    },
};
use ratatui_image::{StatefulImage, protocol::StatefulProtocol};
use sandpolis_client::tui::{EventHandler, MessageBus, Panel, help::HelpWidget};
use sandpolis_network::ServerAddress;
use sandpolis_server::{ServerLayer, client::LoadServerBanner};
use std::{
    ops::Deref,
    sync::{Arc, RwLock},
    time::Duration,
};
use tokio::{
    sync::{
        broadcast,
        mpsc::{self, Receiver},
    },
    task::{JoinHandle, JoinSet},
    time::sleep,
};
use tracing::debug;
use tui_popup::{Popup, SizedWidgetRef};
use tui_prompts::{TextPrompt, TextState};

use super::GRAPHICS;

pub struct ServerListWidget {
    state: Arc<RwLock<ServerListWidgetState>>,
    focused: bool,
    local: MessageBus<LocalMessage>,
}

struct ServerListWidgetState {
    list_state: ListState,
    list_items: Vec<Arc<ServerListItem>>,
    default_banner_image: Option<StatefulProtocol>,
    mode: ServerListWidgetMode,
}

enum ServerListWidgetMode {
    Normal,
    Adding,
    Selecting,
}

impl ServerListWidget {
    pub fn new(server_layer: ServerLayer) -> Self {
        let mut list_items = server_layer
            .network
            .servers
            .iter()
            .map(|connection| {
                Arc::new(ServerListItem::new(
                    server_layer.clone(),
                    connection.address.clone(),
                ))
            })
            .collect::<Vec<Arc<ServerListItem>>>();

        // Add local server if one exists
        #[cfg(feature = "server")]
        {
            list_items.push(Arc::new(ServerListItem {
                address: "127.0.0.1:8768".parse().unwrap(),
                banner: RwLock::new(LoadServerBanner::Loaded(server_layer.banner.read().clone())),
                // The banner is already loaded, so this channel will never be used
                fetch_banner: RwLock::new(mpsc::channel(1).1),
                ping: RwLock::new(Some(0)),
            }));
        }

        let state = Arc::new(RwLock::new(ServerListWidgetState {
            list_items,
            list_state: ListState::default(),
            default_banner_image: GRAPHICS.map(|picker| {
                picker.new_resize_protocol(
                    image::io::Reader::open("/home/cilki/Downloads/sandpolis-256.png")
                        .unwrap()
                        .decode()
                        .unwrap(),
                )
            }),
            mode: ServerListWidgetMode::Normal,
        }));

        let local = broadcast::channel(8);

        Self {
            state,
            focused: true,
            local,
        }
    }
}

impl WidgetRef for ServerListWidget {
    fn render_ref(&self, area: Rect, buf: &mut Buffer) {
        let mut state = self.state.write().unwrap();

        let help_widget = HelpWidget {
            keybindings: vec![
                (KeyCode::Char('a'), "Add a new server".to_string()),
                (KeyCode::Char('X'), "Remove server".to_string()),
                (KeyCode::Right, "Login".to_string()),
            ],
        };
        let block = Block::default()
            .borders(Borders::ALL)
            .title("Server Selection");

        let [banner_area, list_area, help_area] = Layout::vertical([
            Constraint::Percentage(30),
            Constraint::Percentage(70),
            Constraint::Length(help_widget.height()),
        ])
        .areas(block.inner(area));

        block.render(area, buf);

        // Render banner
        StatefulWidget::render(
            StatefulImage::default(),
            banner_area,
            buf,
            state.default_banner_image.as_mut().unwrap(),
        );

        // Render list
        StatefulWidget::render(
            state
                .list_items
                .iter()
                .map(|i| i.deref())
                .collect::<List>()
                .highlight_style(Style::new().white()),
            list_area,
            buf,
            &mut state.list_state,
        );

        // Render help
        if self.focused {
            help_widget.render(help_area, buf);
        }

        // Render dialog if we're in "add" mode
        match state.mode {
            ServerListWidgetMode::Adding => {
                let popup =
                    Popup::new(AddServerWidget::new()).style(Style::new().white().on_blue());
                popup.render(area, buf);
            }
            _ => (),
        }
    }
}

struct ServerListItem {
    address: ServerAddress,
    banner: RwLock<LoadServerBanner>,
    fetch_banner: RwLock<Receiver<LoadServerBanner>>,
    ping: RwLock<Option<u32>>,
}

impl ServerListItem {
    fn new(server_layer: ServerLayer, address: ServerAddress) -> Self {
        Self {
            banner: RwLock::new(LoadServerBanner::Loading),
            fetch_banner: RwLock::new(server_layer.fetch_banner(&address)),
            ping: RwLock::new(None),
            address,
        }
    }

    async fn run(&self) {
        loop {
            match *self.banner.read().unwrap() {
                LoadServerBanner::Loading => {
                    if let Some(progress) = self.fetch_banner.write().unwrap().recv().await {
                        *self.banner.write().unwrap() = progress;
                    }
                }
                LoadServerBanner::Loaded(_) => {
                    sleep(Duration::from_secs(3)).await;
                    // TODO ping
                }
                LoadServerBanner::Inaccessible => {
                    sleep(Duration::from_secs(3)).await;
                    // TODO try again
                }
                _ => {}
            }
        }
    }
}

impl From<&ServerListItem> for ListItem<'_> {
    fn from(item: &ServerListItem) -> Self {
        let mut text = Text::default();

        // Add server name
        text.extend([format!("{}", item.address).blue()]);

        // Add online status
        match &*item.banner.read().unwrap() {
            LoadServerBanner::Loading => text.extend([format!("Loading").gray()]),
            LoadServerBanner::Loaded(server_banner_data) => {
                text.extend([format!("Online").green()])
            }
            LoadServerBanner::Inaccessible => text.extend([format!("Offline")]),
            LoadServerBanner::Failed(error) => text.extend([format!("Error")]),
        }
        ListItem::new(text)
    }
}

impl EventHandler for ServerListWidget {
    fn handle_event(&mut self, event: &Event) {
        if let Event::Key(key) = event {
            if key.kind == KeyEventKind::Press {
                let mut state = self.state.write().unwrap();
                match state.mode {
                    ServerListWidgetMode::Normal => match key.code {
                        KeyCode::Char('j') | KeyCode::Down => {
                            state.list_state.select_next();
                        }
                        KeyCode::Char('k') | KeyCode::Up => {
                            state.list_state.select_previous();
                        }
                        KeyCode::Char('a') => {
                            state.mode = ServerListWidgetMode::Adding;
                            debug!("Entering add server mode");
                        }
                        _ => {}
                    },
                    ServerListWidgetMode::Adding => match key.code {
                        KeyCode::Esc => {
                            state.mode = ServerListWidgetMode::Normal;
                            debug!("Exiting add server mode");
                        }
                        _ => {}
                    },
                    _ => todo!(),
                }
            }
        }
    }
}

impl Panel for ServerListWidget {
    fn set_focus(&mut self, focused: bool) {
        self.focused = focused;
    }
}

#[derive(Debug)]
struct AddServerWidget<'a> {
    username_state: TextState<'a>,
}

impl AddServerWidget<'_> {
    pub fn new() -> Self {
        Self {
            username_state: TextState::new(),
        }
    }
}

impl SizedWidgetRef for AddServerWidget<'_> {
    fn width(&self) -> usize {
        10
    }

    fn height(&self) -> usize {
        10
    }
}

impl WidgetRef for AddServerWidget<'_> {
    fn render_ref(&self, area: Rect, buf: &mut Buffer) {
        TextPrompt::from("Username").render(frame, username_area, &mut self.username_state);
    }
}

#[derive(Clone)]
enum LocalMessage {
    ServerAdded,
}
