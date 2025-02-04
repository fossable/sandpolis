use anyhow::Result;
use std::{
    ffi::OsString,
    fmt::Display,
    net::{SocketAddr, ToSocketAddrs},
    str::FromStr,
};

use serde::{Deserialize, Serialize};

#[cfg(feature = "server")]
pub mod server;

pub(crate) mod messages;

#[derive(Clone)]
pub struct ServerLayer {
    pub banner: Document<ServerBanner>,
}

impl ServerLayer {
    pub fn new(cluster_id: ClusterId, document: Document<ServerLayerData>) -> Result<Self> {
        Ok(Self {
            banner: document.document("banner")?,
        })
    }

    pub fn default_group(&self) -> Result<GroupData> {
        todo!()
    }

    #[cfg(feature = "server")]
    pub fn router() -> Router {
        Router::new().route("/banner", get(banner))
    }

    #[cfg(feature = "client")]
    pub fn banner() -> Result<ServerBanner> {}
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
