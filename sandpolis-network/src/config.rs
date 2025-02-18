use crate::{cli::NetworkCommandLine, ServerAddress};
use sandpolis_instance::OverridableConfig;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct NetworkLayerConfig {
    /// Servers to connect to.
    ///
    /// For GS servers, connections will be established to all given values at
    /// the same time. For LS servers, agents, and clients, only one connection
    /// can be maintained at a time.
    pub servers: Option<Vec<ServerAddress>>,

    /// Instead of maintaining a persistent connection, poll the server on this cron expression
    pub poll: Option<String>,
}

impl OverridableConfig<NetworkCommandLine> for NetworkLayerConfig {
    fn override_cli(&mut self, args: NetworkCommandLine) {
        if let Some(server) = args.server {
            self.servers = Some(server);
        }
    }

    fn override_env(&mut self) {
        match std::env::var("S7S_SERVER") {
            Ok(server) => todo!(),
            Err(_) => todo!(),
        }
    }
}
