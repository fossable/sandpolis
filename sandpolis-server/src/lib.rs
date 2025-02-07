use anyhow::Result;
use sandpolis_database::Document;
use serde::{Deserialize, Serialize};

pub mod cli;

#[cfg(feature = "server")]
pub mod server;

pub(crate) mod messages;

#[derive(Serialize, Deserialize, Clone)]
pub struct ServerLayerData {}

#[derive(Clone)]
pub struct ServerLayer {
    pub data: Document<ServerLayerData>,
    pub banner: Document<ServerBanner>,
}

impl ServerLayer {
    pub fn new(data: Document<ServerLayerData>) -> Result<Self> {
        Ok(Self {
            banner: data.document("banner")?,
            data,
        })
    }

    #[cfg(feature = "client")]
    pub fn get_banner() -> Result<ServerBanner> {
        todo!()
    }
}

/// Response bearing the server's banner
#[cfg(any(feature = "server", feature = "client"))]
#[derive(Serialize, Deserialize, Clone, Default)]
pub struct ServerBanner {
    /// Indicates that only admin users will be allowed to login
    pub maintenance: bool,

    /// The 3-field version of the server
    pub version: String,

    /// A string to display on the login screen
    pub message: Option<String>,

    /// An image to display on the login screen
    #[serde(with = "serde_bytes")]
    pub image: Option<Vec<u8>>, // TODO validate with image decoder
}
