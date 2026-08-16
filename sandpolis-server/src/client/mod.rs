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

impl super::ServerManager {
    pub fn save_server(&self, data: SavedServerData) -> Result<()> {
        self.servers.push(data)?;
        Ok(())
    }

    pub fn remove_server(&self, id: DataIdentifier) -> Result<()> {
        self.servers.remove(id)?;
        Ok(())
    }

    /// Record a fresh auth token (and the user it belongs to) for a saved
    /// server, updating the existing entry rather than adding a duplicate.
    pub fn update_server_token(
        &self,
        url: &ServerUrl,
        user: UserName,
        token: ClientAuthToken,
    ) -> Result<()> {
        for server in self.servers.iter() {
            if server.read().address == *url {
                return server.update(|data| {
                    data.user = user.clone();
                    data.token = token.clone();
                    Ok(())
                });
            }
        }

        self.save_server(SavedServerData {
            address: url.clone(),
            token,
            user,
            _id: DataIdentifier::default(),
            _revision: sandpolis_instance::database::DataRevision::Latest(0),
            _creation: sandpolis_instance::database::DataCreation::default(),
        })
    }

    /// The saved auth token for a server, if a usable (non-empty, unexpired)
    /// one is on record.
    pub fn saved_token(&self, url: &ServerUrl) -> Option<ClientAuthToken> {
        self.servers.iter().find_map(|server| {
            let data = server.read();
            (data.address == *url && data.token.is_usable()).then(|| data.token.clone())
        })
    }
}
