use crate::messages::{GetBannerRequest, GetBannerResponse};
use anyhow::{Result, bail};
use native_db::ToKey;
use native_model::Model;
use sandpolis_core::RealmName;
use sandpolis_core::{ClusterId, InstanceId};
use sandpolis_database::DatabaseLayer;
use sandpolis_database::Resident;
use sandpolis_database::ResidentVec;
use sandpolis_macros::data;
use sandpolis_network::NetworkLayer;
use sandpolis_network::OutboundConnection;
use sandpolis_network::ServerUrl;
use sandpolis_user::ClientAuthToken;
use sandpolis_user::messages::{LoginRequest, LoginResponse};
use serde::{Deserialize, Serialize};
use serde_with::DeserializeFromStr;
use std::sync::Arc;
use std::time::Duration;
use tracing::{debug, info};
use validator::Validate;

#[cfg(feature = "client")]
pub mod client;
pub mod config;
pub mod location;
pub mod messages;
#[cfg(feature = "server")]
pub mod server;

#[data]
#[derive(Default)]
pub struct ServerLayerData {}

#[derive(Clone)]
pub struct ServerLayer {
    #[cfg(feature = "server")]
    pub banner: Resident<server::ServerBannerData>,
    pub network: NetworkLayer,
    #[cfg(feature = "client")]
    pub servers: ResidentVec<client::SavedServerData>,
}

impl ServerLayer {
    pub async fn new(database: DatabaseLayer, network: NetworkLayer) -> Result<Self> {
        Ok(Self {
            #[cfg(feature = "server")]
            banner: database.realm(RealmName::default())?.resident(())?,
            network,
            #[cfg(feature = "client")]
            servers: database.realm(RealmName::default())?.resident_vec(())?,
        })
    }

    /// Get all server connections.
    pub fn server_connections(&self) -> Vec<Arc<OutboundConnection>> {
        let mut connections = self.network.outbound.read().unwrap().clone();
        connections.retain(|connection| connection.data.read().remote_instance.is_server());
        connections
    }

    #[cfg(any(feature = "agent", feature = "client"))] // Temporary
    pub async fn connect(&self, url: ServerUrl) -> Result<ServerConnection> {
        let inner = self.network.connect_server(url)?;

        debug!("Fetching server banner");

        // Fetch banner before we return a complete connection
        let response: GetBannerResponse = inner
            .get(
                "server/banner",
                GetBannerRequest {
                    #[cfg(feature = "client-gui")]
                    include_image: true,
                    #[cfg(not(feature = "client-gui"))]
                    include_image: false,
                },
            )
            .await?;

        debug!(banner = ?response.0, "Fetched server banner");

        Ok(ServerConnection {
            inner,
            banner: response.0,
        })
    }
}

/// Contains information about the server useful for prospective logins
#[derive(Serialize, Deserialize, Debug, Clone, PartialEq, Default)]
pub struct ServerBanner {
    /// Indicates that only admin users will be allowed to login
    pub maintenance: bool,

    /// A string to display on the login screen
    pub message: Option<String>,

    /// An image to display on the login screen
    #[serde(with = "serde_bytes")]
    pub image: Option<Vec<u8>>,

    /// Whether users are required to provide a second authentication mechanism
    /// on login
    pub mfa: bool,
}

impl Validate for ServerBanner {
    fn validate(&self) -> Result<(), validator::ValidationErrors> {
        if let Some(image_data) = &self.image {
            // Validate PNG format using png crate
            let cursor = std::io::Cursor::new(image_data);
            let decoder = png::Decoder::new(cursor);

            if decoder.read_info().is_err() {
                return Err(validator::ValidationErrors::new());
            }
        }

        Ok(())
    }
}

pub struct ServerConnection {
    pub inner: OutboundConnection,
    pub banner: ServerBanner,
}

impl ServerConnection {
    pub async fn login(&self, request: LoginRequest) -> Result<LoginResponse> {
        // TODO span username
        debug!(username = %request.username, "Attempting login");

        let result = self.inner.post("user/login", request).await;
        match result {
            Ok(LoginResponse::Ok(_)) => {
                info!("Login succeeded");
            }
            _ => {}
        }
        result
    }
}

/// A group is a collection of instances within the same realm.
#[data]
pub struct GroupData {
    #[secondary_key(unique)]
    pub name: String,

    pub members: Vec<InstanceId>,
}
