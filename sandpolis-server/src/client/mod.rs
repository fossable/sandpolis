use anyhow::Result;
use native_db::ToKey;
use native_model::Model;
use sandpolis_core::UserName;
use sandpolis_database::DataIdentifier;
use sandpolis_macros::data;
use crate::ServerUrl;
use sandpolis_user::ClientAuthToken;

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

    pub fn remove_server(&self, id: DataIdentifier) -> Result<()> {
        self.servers.remove(id)?;
        Ok(())
    }
}
