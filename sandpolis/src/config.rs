use crate::cli::CommandLine;
use anyhow::{bail, Result};
use serde::{Deserialize, Serialize};
use std::{fs::File, path::PathBuf};

/// Application's global config.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct Configuration {
    #[cfg(feature = "agent")]
    pub agent: sandpolis_agent::config::AgentLayerConfig,
    #[cfg(feature = "client")]
    pub client: sandpolis_client::config::ClientLayerConfig,
    pub database: sandpolis_database::config::DatabaseConfig,
    pub group: sandpolis_group::config::GroupConfig,
    #[cfg(feature = "layer-location")]
    pub location: sandpolis_location::config::LocationConfig,
    pub network: sandpolis_network::config::NetworkLayerConfig,
    #[cfg(feature = "server")]
    pub server: sandpolis_server::config::ServerLayerConfig,
    #[cfg(feature = "layer-snapshot")]
    pub snapshot: sandpolis_snapshot::config::SnapshotConfig,
}

impl Configuration {
    pub fn new(args: &CommandLine) -> Result<Self> {
        // Attempt to read from config file
        let path = args.instance.config.clone().unwrap_or(PathBuf::from(
            std::env::var("S7S_CONFIG").ok().unwrap_or("TODO".into()),
        ));

        let config = match File::open(&path) {
            Ok(mut file) => match path
                .extension()
                .expect("config file has a file extension")
                .to_string_lossy()
                .to_string()
                .as_str()
            {
                "json" => serde_json::from_reader(&mut file)?,
                _ => bail!("Unknown configuration file type"),
            },
            Err(error) => match error.kind() {
                std::io::ErrorKind::NotFound => Configuration::default(),
                std::io::ErrorKind::PermissionDenied => todo!(),
                _ => todo!(),
            },
        };

        Ok(config)
    }
}
