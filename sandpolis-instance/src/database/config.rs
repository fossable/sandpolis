use anyhow::{Result, bail};
use base64::{Engine, engine::general_purpose::STANDARD as BASE64};
use rand::Rng;
use serde::{Deserialize, Serialize};
use std::path::PathBuf;
use validator::Validate;

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct DatabaseConfig {
    /// Override default storage directory
    pub storage: Option<PathBuf>,

    /// Don't persist any data
    pub ephemeral: bool,

    /// Key that encrypts the entire database
    pub key: DatabaseKey,
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
            key: DatabaseKey::default(),
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
        let key: [u8; 32] = rand::rng().random();
        Self::Plaintext(BASE64.encode(key))
    }
}
