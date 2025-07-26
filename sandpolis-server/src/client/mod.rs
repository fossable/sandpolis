use crate::{ServerBanner, ServerLayer};
use anyhow::{Error, Result};
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

impl super::ServerLayer {
    pub fn save_server(&self, data: SavedServerData) -> Result<()> {
        self.servers.push(data)?;
        Ok(())
    }
}
