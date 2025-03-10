use crate::cli::CommandLine;
use anyhow::{bail, Result};
use serde::{Deserialize, Serialize};
use std::{fs::File, path::PathBuf};
use tracing::debug;

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
    /// Default config file location
    fn default_path() -> String {
        if cfg!(target_os = "linux") {
            "~/.config/sandpolis"
        } else {
            panic!()
        }
        .into()
    }

    pub fn new(args: &CommandLine) -> Result<Self> {
        // Attempt to read from embedded config
        let config_bytes = include_bytes!("../config.bin");
        if config_bytes.len() != 63 {
            let config: Configuration = serde_json::from_slice(config_bytes)?;
            debug!(config = ?config, "Read embedded configuration");
            return Ok(config);
        }

        // Attempt to read from config file
        let path = args.instance.config.clone().unwrap_or(PathBuf::from(
            std::env::var("S7S_CONFIG")
                .ok()
                .unwrap_or(Configuration::default_path()),
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
