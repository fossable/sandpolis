use std::{ffi::OsString, fmt::Display};

use serde::{Deserialize, Serialize};

pub mod group;
pub mod user;

/// Indicates what level the server is from.
#[derive(Serialize, Deserialize, Clone, Debug)]
pub enum ServerStratum {
    /// This server maintains data only for the agents that its directly connected
    /// to. Local stratum (LS) servers connect to at most one GS server. LS servers
    /// may not connect directly to each other.
    ///
    /// LS servers are optional, but may be useful for on-premise installations
    /// where the server can continue operating even when the network goes down.
    Local,

    /// This server maintains a complete copy of all data in the cluster. Global
    /// stratum (GS) servers connect to every other GS server (fully-connected)
    /// for data replication and leader election.
    Global,
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

#[cfg(any(feature = "server", feature = "client"))]
#[derive(Serialize, Deserialize)]
pub struct GetBannerRequest;

#[cfg(any(feature = "server", feature = "client"))]
#[derive(Serialize, Deserialize)]
pub enum GetBannerResponse {
    Ok(ServerBanner),
}
