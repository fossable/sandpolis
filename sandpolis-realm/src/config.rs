use crate::RealmClientCert;
use anyhow::{Context, Result};
use serde::{Deserialize, Serialize};
use std::path::PathBuf;

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct RealmConfig {
    /// Path to authentication certificate which will be installed into the
    /// database. Subsequent runs don't require this option.
    pub certificate: Option<PathBuf>,
}

impl RealmConfig {
    pub fn certificate(&self) -> Result<Option<RealmClientCert>> {
        if let Some(path) = self.certificate.as_ref() {
            let cert = std::fs::read(path).context("Failed to read certificate")?;
            Ok(serde_json::from_slice(&cert)
                .context(format!("Failed to parse certificate: {:?}", path))?)
        } else {
            Ok(None)
        }
    }
}
