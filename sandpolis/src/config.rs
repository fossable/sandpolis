use anyhow::{Result, bail};
use fs2::FileExt;
use serde::{Deserialize, Serialize};
use std::fs::OpenOptions;
use std::io::{Read, Seek, SeekFrom, Write};
use std::path::PathBuf;
use tracing::debug;

/// Application's global config.
#[cfg_attr(feature = "client", derive(bevy::prelude::Resource))]
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(default)]
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
    #[cfg(feature = "server")]
    pub server: sandpolis_server::config::ServerLayerConfig,
    #[cfg(feature = "layer-snapshot")]
    pub snapshot: sandpolis_snapshot::config::SnapshotConfig,
    #[cfg(feature = "layer-probe")]
    pub probe: sandpolis_probe::config::ProbeLayerConfig,
}

/// RON parsing options for config files: allow optional fields without an
/// explicit `Some`.
fn ron_options() -> ron::Options {
    ron::Options::default()
        .with_default_extension(ron::extensions::Extensions::IMPLICIT_SOME)
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

    /// Load configuration.
    ///
    /// `config_path` should come from the `--config` flag on the `server`
    /// subcommand (server instances are the only ones with a writable on-disk
    /// config). Falls back to `$S7S_CONFIG` then the platform default.
    pub fn new(config_path: Option<PathBuf>) -> Result<Self> {
        // For agent instances, prefer the embedded config if present
        #[cfg(feature = "agent")]
        {
            const EMBEDDED_CONFIG: &[u8] = include_bytes!("../config.ron");
            const PLACEHOLDER: &[u8] =
                b"// Replace this file to embed a config in the application binary.\n";

            if EMBEDDED_CONFIG != PLACEHOLDER {
                debug!("Loading embedded configuration");
                let config: Configuration =
                    ron_options().from_str(std::str::from_utf8(EMBEDDED_CONFIG)?)?;
                return Ok(config);
            }
        }

        let path = config_path
            .or_else(|| std::env::var("S7S_CONFIG").ok().map(PathBuf::from))
            .unwrap_or_else(Self::default_path);

        debug!(path = %path.display(), "Loading configuration");

        let mut config: Configuration = match std::fs::read_to_string(&path) {
            Ok(contents) => ron_options().from_str(&contents)?,
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
    ///
    /// Acquires an exclusive advisory lock on the config file for the duration
    /// of the write so concurrent `sandpolis` processes can't clobber each
    /// other. The lock is released when the file handle is dropped.
    pub fn save(&self) -> Result<()> {
        let Some(path) = &self.path else {
            return Ok(());
        };

        debug!(path = %path.display(), "Saving configuration");

        if let Some(parent) = path.parent() {
            std::fs::create_dir_all(parent)?;
        }

        let contents = ron::ser::to_string_pretty(self, ron::ser::PrettyConfig::default())?;

        let mut file = OpenOptions::new()
            .create(true)
            .read(true)
            .write(true)
            .truncate(false)
            .open(path)?;

        FileExt::lock_exclusive(&file)?;
        file.set_len(0)?;
        file.seek(SeekFrom::Start(0))?;
        file.write_all(contents.as_bytes())?;
        file.sync_all()?;

        Ok(())
    }

    /// Read-modify-write the on-disk config under an exclusive lock.
    ///
    /// Re-reads the file from disk after acquiring the lock so the closure
    /// always sees the latest committed state, then writes the mutated value
    /// back before releasing the lock. Use this whenever multiple processes
    /// may be racing to update the same config.
    pub fn modify<F>(&mut self, f: F) -> Result<()>
    where
        F: FnOnce(&mut Configuration) -> Result<()>,
    {
        let Some(path) = self.path.clone() else {
            bail!("Configuration has no associated file path");
        };

        if let Some(parent) = path.parent() {
            std::fs::create_dir_all(parent)?;
        }

        let mut file = OpenOptions::new()
            .create(true)
            .read(true)
            .write(true)
            .truncate(false)
            .open(&path)?;

        FileExt::lock_exclusive(&file)?;

        let mut buf = String::new();
        file.read_to_string(&mut buf)?;
        if !buf.is_empty() {
            *self = ron_options().from_str(&buf)?;
            self.path = Some(path);
        }

        f(self)?;

        let contents = ron::ser::to_string_pretty(self, ron::ser::PrettyConfig::default())?;
        file.set_len(0)?;
        file.seek(SeekFrom::Start(0))?;
        file.write_all(contents.as_bytes())?;
        file.sync_all()?;

        Ok(())
    }
}
