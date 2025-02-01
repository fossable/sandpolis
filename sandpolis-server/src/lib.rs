use anyhow::Result;
use std::{
    ffi::OsString,
    fmt::Display,
    net::{SocketAddr, ToSocketAddrs},
    str::FromStr,
};

use serde::{Deserialize, Serialize};

/// There can be multiple servers in a Sandpolis network which improves both
/// scalability and failure tolerance. There are two roles in which a server can
/// exist: a global level and a local level.
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

#[derive(Serialize, Deserialize, Clone, Debug)]
pub enum ServerAddress {
    Dns(String),
    Ip(SocketAddr),
}

impl ServerAddress {
    pub fn resolve(&self) -> Result<Vec<SocketAddr>> {
        match self {
            Self::Dns(name) => Ok(name.to_socket_addrs()?.collect()),
            Self::Ip(socket_addr) => Ok(vec![socket_addr.clone()]),
        }
    }

    /// Official server port: <https://www.iana.org/assignments/service-names-port-numbers/service-names-port-numbers.xhtml?search=8768>
    pub const fn default_port() -> u16 {
        8768
    }
}

impl FromStr for ServerAddress {
    type Err = anyhow::Error;

    fn from_str(s: &str) -> Result<Self> {
        match s.parse::<SocketAddr>() {
            Ok(addr) => Ok(Self::Ip(addr)),
            // TODO regex
            Err(_) => Ok(Self::Dns(s.to_string())),
        }
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

#[cfg(any(feature = "server", feature = "client"))]
#[derive(Serialize, Deserialize)]
pub struct GetBannerRequest;

#[cfg(any(feature = "server", feature = "client"))]
#[derive(Serialize, Deserialize)]
pub enum GetBannerResponse {
    Ok(ServerBanner),
}
