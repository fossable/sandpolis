use anyhow::{bail, Result};
use serde::{Deserialize, Serialize};
use std::path::PathBuf;

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct DatabaseConfig {
    /// Storage directory
    pub storage: PathBuf,
}

impl Default for DatabaseConfig {
    fn default() -> Self {
        Self {
            storage: "/tmp".into(),
        }
    }
}

impl DatabaseConfig {
    /// Create the storage directory if needed.
    pub fn create_storage_dir(&self) -> Result<()> {
        if !std::fs::exists(&self.storage)? {
            std::fs::create_dir_all(&self.storage)?;
        }
        if !std::fs::metadata(&self.storage)?.is_dir() {
            bail!("Storage directory must be a directory");
        }
        Ok(())
    }
}
