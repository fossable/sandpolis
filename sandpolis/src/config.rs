use crate::cli::CommandLine;
use anyhow::{Result, bail};
use sandpolis_core::LayerConfig;
use serde::{Deserialize, Serialize};
use std::{fs::File, path::PathBuf};
use tracing::debug;

/// Application's global config.
#[cfg_attr(feature = "client-gui", derive(bevy::prelude::Resource))]
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct Configuration {
    #[cfg(feature = "agent")]
    pub agent: sandpolis_agent::config::AgentLayerConfig,
    #[cfg(feature = "client")]
    pub client: sandpolis_client::config::ClientLayerConfig,
    pub database: sandpolis_database::config::DatabaseConfig,
    /// Whether overrides from environment variables or the command line are
    /// allowed
    pub disable_overrides: bool,
    pub instance: sandpolis_instance::config::InstanceConfig,
    pub network: sandpolis_network::config::NetworkLayerConfig,
    pub realm: sandpolis_realm::config::RealmConfig,
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
        let embedded_config = include_bytes!("../config.bin");
        let mut config: Configuration = if embedded_config.len()
            != "Replace this file to embed a config in the application binary.\n".len()
        {
            let config: Configuration = serde_json::from_slice(embedded_config)?;
            debug!(config = ?config, "Loading embedded configuration");
            config
        } else {
            // Attempt to read from config file
            let path = args.instance.config.clone().unwrap_or(PathBuf::from(
                std::env::var("S7S_CONFIG")
                    .ok()
                    .unwrap_or(Configuration::default_path()),
            ));

            debug!(path = %path.display(), "Loading file configuration");
            match File::open(&path) {
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
            }
        };

        // Handle overrides if allowed
        if !config.disable_overrides {
            config.instance.override_cli(&args.instance);
            config.database.override_cli(&args.database);
            config.network.override_cli(&args.network);
            config.realm.override_cli(&args.realm);
        }

        Ok(config)
    }
}
