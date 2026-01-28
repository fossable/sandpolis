use super::cli::DatabaseCommandLine;
use anyhow::{Result, bail};
use sandpolis_instance::LayerConfig;
use serde::{Deserialize, Serialize};
use std::path::PathBuf;
use tracing::trace;
use validator::Validate;

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct DatabaseConfig {
    /// Override default storage directory
    pub storage: Option<PathBuf>,

    /// Don't persist any data
    pub ephemeral: bool,
}

impl Validate for DatabaseConfig {
    fn validate(&self) -> std::result::Result<(), validator::ValidationErrors> {
        if self.ephemeral && self.storage.is_some() {
            // TODO don't allow
        }

        Ok(())
    }
}

impl Default for DatabaseConfig {
    fn default() -> Self {
        Self {
            // TODO platform specific
            storage: Some("/tmp".into()),
            ephemeral: false,
        }
    }
}

impl LayerConfig<DatabaseCommandLine> for DatabaseConfig {
    fn override_cli(&mut self, args: &DatabaseCommandLine) {
        if args.ephemeral {
            trace!("Overriding ephemeral flag from CLI");
            self.ephemeral = true;
            self.storage = None;
        }
    }
}

impl DatabaseConfig {
    /// Create the storage directory if needed.
    pub fn get_storage_dir(&self) -> Result<Option<PathBuf>> {
        if let Some(path) = self.storage.clone() {
            if !std::fs::exists(&path)? {
                std::fs::create_dir_all(&path)?;
            } else if !std::fs::metadata(&path)?.is_dir() {
                bail!("Storage directory must be a directory");
            }
            Ok(Some(path))
        } else if self.ephemeral {
            Ok(None)
        } else {
            let path = Self::default().storage.unwrap();
            if !std::fs::exists(&path)? {
                std::fs::create_dir_all(&path)?;
            } else if !std::fs::metadata(&path)?.is_dir() {
                bail!("Storage directory must be a directory");
            }
            Ok(Some(path))
        }
    }
}
