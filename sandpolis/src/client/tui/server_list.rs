use std::{sync::Arc, sync::RwLock, time::Duration};

use ratatui::{
    layout::{Constraint, Direction, Layout},
    style::Stylize,
    text::Text,
    widgets::{Block, Borders, List, ListItem, ListState, StatefulWidget, Widget, WidgetRef},
};
use sandpolis_network::ServerAddress;
use sandpolis_server::{ServerLayer, client::LoadServerBanner};
use tokio::{
    sync::mpsc::{self, Receiver},
    task::{JoinHandle, JoinSet},
    time::sleep,
};

pub struct ServerListWidget {
    list_items: Arc<RwLock<Vec<ServerListItem>>>,
    list_state: ListState,
    focused: bool,
    threads: JoinSet<()>,
}

impl ServerListWidget {
    pub fn new(server_layer: ServerLayer) -> Self {
        let list_items = Arc::new(RwLock::new(
            server_layer
                .network
                .servers
                .iter()
                .map(|connection| {
                    ServerListItem::new(server_layer.clone(), connection.address.clone())
                })
                .collect::<Vec<ServerListItem>>(),
        ));

        // Add local server if one exists
        #[cfg(feature = "server")]
        {
            list_items.write().unwrap().push(ServerListItem {
                address: todo!(),
                banner: RwLock::new(LoadServerBanner::Loaded(server_layer.banner.data.clone())),
                // The banner is already loaded, so this channel will never be used
                fetch_banner: RwLock::new(mpsc::channel(1).1),
                ping: RwLock::new(Some(0)),
            });
        }

        let mut threads = JoinSet::new();
        // for (i, item) in list_items.read().unwrap().iter().enumerate() {
        //     let items_clone = list_items.clone();
        //     threads.spawn(async move { items_clone.read().unwrap()[i].run().await });
        // }

        Self {
            list_items,
            list_state: ListState::default(),
            focused: true,
            threads,
        }
    }
}

impl WidgetRef for &ServerListWidget {
    fn render_ref(&self, area: ratatui::layout::Rect, buf: &mut ratatui::buffer::Buffer) {
        let outer_block = Block::default().borders(Borders::TOP).title("Servers");

        let layout = Layout::default()
            .direction(Direction::Vertical)
            .constraints([Constraint::Percentage(10), Constraint::Percentage(90)].as_ref())
            .split(outer_block.inner(area));
        outer_block.render(area, buf);

        let banner_area = layout[0];
        let list_area = layout[1];

        // self.list_items
        //     .read()
        //     .unwrap()
        //     .iter()
        //     .collect::<List>()
        //     .render(list_area, buf, &mut self.list_state);
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
        text.extend(["Item".blue(), "1".bold().red()]);
        ListItem::new(text)
    }
}
