use sandpolis_instance::network::cli::NetworkCommandLine;
use sandpolis_instance::LayerConfig;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct NetworkLayerConfig {
    /// Instead of maintaining a persistent connection, poll the server on this
    /// cron expression
    #[cfg(not(feature = "bootagent"))]
    pub poll: Option<String>,

    /// Set a static IP address instead of using DHCP.
    #[cfg(feature = "bootagent")]
    pub interface_ip: Option<String>,

    /// Use the interface with this MAC address.
    #[cfg(feature = "bootagent")]
    pub interface_mac: Option<String>,
}

impl LayerConfig<NetworkCommandLine> for NetworkLayerConfig {
    fn override_env(&mut self) {}

    fn override_cli(&mut self, _args: &NetworkCommandLine) {}
}
