use ratatui::{
    crossterm::event::{Event, KeyCode, KeyEventKind},
    layout::{Constraint, Direction, Layout},
    style::Stylize,
    text::Text,
    widgets::{
        Block, Borders, List, ListItem, ListState, StatefulWidget, StatefulWidgetRef, Widget,
        WidgetRef,
    },
};
use ratatui_image::{StatefulImage, protocol::StatefulProtocol};
use sandpolis_client::tui::{EventHandler, Panel};
use sandpolis_network::ServerAddress;
use sandpolis_server::{ServerLayer, client::LoadServerBanner};
use std::{sync::Arc, sync::RwLock, time::Duration};
use tokio::{
    sync::mpsc::{self, Receiver},
    task::{JoinHandle, JoinSet},
    time::sleep,
};

pub struct ServerListWidget {
    state: Arc<RwLock<ServerListWidgetState>>,
    focused: bool,
    threads: JoinSet<()>,
}

struct ServerListWidgetState {
    list_state: ListState,
    list_items: Vec<ServerListItem>,
    // default_banner_image: StatefulProtocol,
}

impl ServerListWidget {
    pub fn new(server_layer: ServerLayer) -> Self {
        let mut list_items = server_layer
            .network
            .servers
            .iter()
            .map(|connection| ServerListItem::new(server_layer.clone(), connection.address.clone()))
            .collect::<Vec<ServerListItem>>();

        // Add local server if one exists
        #[cfg(feature = "server")]
        {
            list_items.push(ServerListItem {
                address: "127.0.0.1:8768".parse().unwrap(),
                banner: RwLock::new(LoadServerBanner::Loaded(server_layer.banner.data.clone())),
                // The banner is already loaded, so this channel will never be used
                fetch_banner: RwLock::new(mpsc::channel(1).1),
                ping: RwLock::new(Some(0)),
            });
        }

        let state = Arc::new(RwLock::new(ServerListWidgetState {
            list_items,
            list_state: ListState::default(),
            // default_banner_image: todo!(),
        }));

        let mut threads = JoinSet::new();
        // for (i, item) in list_items.read().unwrap().iter().enumerate() {
        //     let state_clone = state.clone();
        //     threads.spawn(async move { items_clone.read().unwrap()[i].run().await });
        // }

        Self {
            state,
            focused: true,
            threads,
        }
    }
}

impl WidgetRef for &ServerListWidget {
    fn render_ref(&self, area: ratatui::layout::Rect, buf: &mut ratatui::buffer::Buffer) {
        let mut state = self.state.write().unwrap();
        let outer_block = Block::default().borders(Borders::TOP).title("Servers");

        let layout = Layout::default()
            .direction(Direction::Vertical)
            .constraints([Constraint::Percentage(10), Constraint::Percentage(90)].as_ref())
            .split(outer_block.inner(area));
        outer_block.render(area, buf);

        let banner_area = layout[0];
        StatefulWidget::render(StatefulImage::default(), banner_area, buf, todo!());

        let list_area = layout[1];

        StatefulWidget::render(
            state
                .list_items
                .iter()
                .collect::<List>()
                .highlight_symbol(">>>"),
            list_area,
            buf,
            &mut state.list_state,
        );
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
        text.extend([format!("{}", item.address).blue(), "1".bold().red()]);
        ListItem::new(text)
    }
}

impl EventHandler for &ServerListWidget {
    fn handle_event(&self, event: &Event) {
        if let Event::Key(key) = event {
            if key.kind == KeyEventKind::Press {
                match key.code {
                    KeyCode::Char('j') | KeyCode::Down => {
                        self.state.write().unwrap().list_state.select_next();
                    }
                    KeyCode::Char('k') | KeyCode::Up => {
                        self.state.write().unwrap().list_state.select_previous();
                    }
                    _ => {}
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
