use sandpolis_network::ServerUrl;
use serde::Deserialize;
use serde::Serialize;
use std::net::IpAddr;
use std::net::Ipv4Addr;
use std::net::SocketAddr;

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct ServerLayerConfig {
    /// Server listen address:port
    pub listen: SocketAddr,

    /// Run as a local stratum (LS) server instead of in the global stratum
    /// (GS).
    pub local: bool,

    /// Service to use for resolving IP location info
    #[cfg(feature = "server")]
    pub service: crate::location::server::LocationService,
}

impl Default for ServerLayerConfig {
    fn default() -> Self {
        Self {
            listen: SocketAddr::new(
                IpAddr::V4(Ipv4Addr::new(0, 0, 0, 0)),
                ServerUrl::default_port(),
            ),
            local: false,
            #[cfg(feature = "server")]
            service: crate::location::server::LocationService::default(),
        }
    }
}
