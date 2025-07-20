use crate::{ServerBannerData, ServerLayer};
use anyhow::Error;
use native_db::ToKey;
use native_model::Model;
use sandpolis_core::UserName;
use sandpolis_macros::data;
use sandpolis_network::ServerUrl;
use sandpolis_user::ClientAuthToken;
use tokio::sync::mpsc::{self, Receiver};

/// Clients can save servers to make subsequent logins faster.
#[data]
pub struct SavedServerData {
    pub address: ServerUrl,
    pub token: ClientAuthToken,
    pub user: UserName,
}

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
