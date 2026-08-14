use anyhow::{Result, bail};
use base64::{Engine, engine::general_purpose::STANDARD as BASE64};
use rand::Rng;
use serde::{Deserialize, Serialize};
use std::path::PathBuf;

#[derive(Serialize, Deserialize, Debug, Clone, Default)]
pub struct DatabaseConfig {
    /// Storage directory, set via the `--data` flag. Not part of the on-disk
    /// config.
    ///
    /// Absent means the instance is ephemeral: every realm's database is kept
    /// in memory and nothing survives the process.
    #[serde(skip)]
    pub storage: Option<PathBuf>,

    /// Key that encrypts the entire database
    pub key: DatabaseKey,
}

impl DatabaseConfig {
    /// The storage directory, created if it doesn't exist yet. `None` when this
    /// instance is ephemeral.
    pub fn get_storage_dir(&self) -> Result<Option<PathBuf>> {
        let Some(path) = self.storage.clone() else {
            return Ok(None);
        };

        if !std::fs::exists(&path)? {
            std::fs::create_dir_all(&path)?;
        } else if !std::fs::metadata(&path)?.is_dir() {
            bail!("Storage directory must be a directory");
        }
        Ok(Some(path))
    }
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub enum DatabaseKey {
    /// Just the unprotected database key.
    Plaintext(String),
    /// Run a command to get the database key.
    Command(String),
}

impl Default for DatabaseKey {
    fn default() -> Self {
        // Generate a 256-bit cryptographically secure key
        let mut key = [0u8; 32];
        rand::rng().fill_bytes(&mut key);
        Self::Plaintext(BASE64.encode(key))
    }
}
