use std::sync::Arc;

use anyhow::Result;
use native_db::ToKey;
use native_model::Model;
#[cfg(feature = "server")]
use sandpolis_core::RealmName;
use sandpolis_core::{ClusterId, InstanceId};
use sandpolis_database::DatabaseLayer;
use sandpolis_database::Resident;
use sandpolis_macros::data;
use sandpolis_network::NetworkLayer;
use sandpolis_network::OutboundConnection;
use sandpolis_network::ServerUrl;

#[cfg(feature = "client")]
pub mod client;
pub mod config;
pub mod location;
pub mod messages;
#[cfg(feature = "server")]
pub mod server;

#[data]
pub struct ServerLayerData {}

#[derive(Clone)]
pub struct ServerLayer {
    #[cfg(feature = "server")]
    pub banner: Resident<ServerBannerData>,
    pub network: NetworkLayer,
}

impl ServerLayer {
    pub async fn new(database: DatabaseLayer, network: NetworkLayer) -> Result<Self> {
        Ok(Self {
            #[cfg(feature = "server")]
            banner: database.realm(RealmName::default())?.resident(())?,
            network,
        })
    }

    /// Get all server connections.
    pub fn server_connections(&self) -> Vec<Arc<OutboundConnection>> {
        let mut connections = self.network.outbound.read().unwrap().clone();
        connections.retain(|connection| connection.data.read().remote_instance.is_server());
        connections
    }

    pub fn get_banner() -> Result<ServerBannerData> {
        todo!()
    }

    pub fn add_server(&self, server_url: ServerUrl) -> Result<()> {
        todo!()
    }
}

/// Contains information about the server useful for prospective logins
#[data]
pub struct ServerBannerData {
    pub cluster_id: ClusterId,

    /// Indicates that only admin users will be allowed to login
    pub maintenance: bool,

    /// A string to display on the login screen
    pub message: Option<String>,

    /// An image to display on the login screen
    #[serde(with = "serde_bytes")]
    pub image: Option<Vec<u8>>, // TODO validate with image decoder

    /// Whether users are required to provide a second authentication mechanism
    /// on login
    pub mfa: bool,
}

/// A group is a collection of instances within the same realm.
#[data]
pub struct GroupData {
    #[secondary_key(unique)]
    pub name: String,

    pub members: Vec<InstanceId>,
}
