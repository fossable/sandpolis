use anyhow::Result;
use sandpolis_core::ClusterId;
use sandpolis_database::DatabaseLayer;
use sandpolis_network::{NetworkLayer, ServerAddress};
use serde::{Deserialize, Serialize};

pub mod config;

#[cfg(feature = "server")]
pub mod server;

#[cfg(feature = "client")]
pub mod client;

pub mod messages;

#[derive(Serialize, Deserialize, Clone, Default)]
pub struct ServerLayerData {}

#[derive(Clone)]
pub struct ServerLayer {
    database: DatabaseLayer,
    #[cfg(feature = "server")]
    pub banner: Document<ServerBannerData>,
    pub network: NetworkLayer,
}

impl ServerLayer {
    pub fn new(database: DatabaseLayer, network: NetworkLayer) -> Result<Self> {
        Ok(Self {
            #[cfg(feature = "server")]
            banner: if let Some(document) = data.get_document("/banner")? {
                document
            } else {
                // Load banner from another server if there is one
                // TODO

                // Create a new banner
                data.document("/banner")?
            },
            database,
            network,
        })
    }

    pub fn get_banner() -> Result<ServerBannerData> {
        todo!()
    }
}

/// Response bearing the server's banner
#[derive(Serialize, Deserialize, Clone, Default)]
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
