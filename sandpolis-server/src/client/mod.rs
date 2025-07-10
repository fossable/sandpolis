use anyhow::Error;
use sandpolis_network::ServerUrl;
use tokio::sync::mpsc::{self, Receiver};

use crate::{ServerBannerData, ServerLayer};

#[derive(Default)]
pub enum LoadServerBanner {
    /// Currently attempting to connect
    #[default]
    Loading,

    /// Server banner was fetched successfully
    Loaded(ServerBannerData),

    /// Server instance could not be reached
    Inaccessible,

    /// Generally failed to fetch server banner
    Failed(Error),
}

impl ServerLayer {
    pub fn fetch_banner(&self, server: &ServerUrl) -> Receiver<LoadServerBanner> {
        let (tx, rx) = mpsc::channel(5);

        rx
    }
}
