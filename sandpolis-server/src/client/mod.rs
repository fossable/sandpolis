use crate::ServerUrl;
use crate::user::ClientAuthToken;
use crate::user::UserName;
use anyhow::Result;
use native_db::ToKey;
use native_model::Model;
use sandpolis_instance::database::DataIdentifier;
use sandpolis_macros::data;

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
