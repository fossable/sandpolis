use crate::cli::CommandLine;
use anyhow::Result;
use serde::{Deserialize, Serialize};
use std::path::PathBuf;
use tracing::debug;

/// Application's global config.
#[cfg_attr(feature = "client-gui", derive(bevy::prelude::Resource))]
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct Configuration {
    /// Path to the config file (not serialized)
    #[serde(skip)]
    path: Option<PathBuf>,

    #[cfg(feature = "agent")]
    pub agent: sandpolis_agent::config::AgentLayerConfig,
    #[cfg(feature = "client")]
    pub client: sandpolis_client::config::ClientLayerConfig,
    pub database: sandpolis_instance::database::config::DatabaseConfig,
    pub instance: sandpolis_instance::config::InstanceConfig,
    pub network: sandpolis_instance::network::config::NetworkLayerConfig,
    pub realm: sandpolis_instance::realm::config::RealmConfig,
    pub server: sandpolis_server::config::ServerLayerConfig,
    #[cfg(feature = "layer-snapshot")]
    pub snapshot: sandpolis_snapshot::config::SnapshotConfig,
}

impl Configuration {
    /// Default config file location
    fn default_path() -> PathBuf {
        if cfg!(target_os = "linux") {
            dirs::config_dir()
                .unwrap_or_else(|| PathBuf::from("~/.config"))
                .join("sandpolis.ron")
        } else {
            panic!("Unsupported platform")
        }
    }

    pub fn new(args: &CommandLine) -> Result<Self> {
        // For agent instances, prefer the embedded config if present
        #[cfg(feature = "agent")]
        {
            const EMBEDDED_CONFIG: &[u8] = include_bytes!("../config.ron");
            const PLACEHOLDER: &[u8] =
                b"// Replace this file to embed a config in the application binary.\n";

            if EMBEDDED_CONFIG != PLACEHOLDER {
                debug!("Loading embedded configuration");
                let config: Configuration = ron::from_str(std::str::from_utf8(EMBEDDED_CONFIG)?)?;
                return Ok(config);
            }
        }

        let path = args
            .instance
            .config
            .clone()
            .or_else(|| std::env::var("S7S_CONFIG").ok().map(PathBuf::from))
            .unwrap_or_else(Self::default_path);

        debug!(path = %path.display(), "Loading configuration");

        let mut config: Configuration = match std::fs::read_to_string(&path) {
            Ok(contents) => ron::from_str(&contents)?,
            Err(error) => match error.kind() {
                std::io::ErrorKind::NotFound => {
                    debug!("Config file not found, using defaults");
                    Configuration::default()
                }
                _ => return Err(error.into()),
            },
        };

        config.path = Some(path);
        Ok(config)
    }

    /// Write the configuration back to the file it was loaded from.
    pub fn save(&self) -> Result<()> {
        if let Some(path) = &self.path {
            debug!(path = %path.display(), "Saving configuration");

            // Create parent directories if needed
            if let Some(parent) = path.parent() {
                std::fs::create_dir_all(parent)?;
            }

            let contents = ron::ser::to_string_pretty(self, ron::ser::PrettyConfig::default())?;
            std::fs::write(path, contents)?;
        }
        Ok(())
    }
}
