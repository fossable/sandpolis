use crate::GroupClientCert;
use anyhow::Result;
use serde::{Deserialize, Serialize};
use std::path::PathBuf;

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct GroupConfig {
    /// Path to authentication certificate which will be installed into the database.
    /// Subsequent runs don't require this option.
    pub certificate: Option<PathBuf>,
}

impl GroupConfig {
    pub fn certificate(&self) -> Result<Option<GroupClientCert>> {
        if let Some(cert) = self.certificate.as_ref() {
            let cert = std::fs::read(cert)?;
            Ok(serde_json::from_slice(&cert)?)
        } else {
            Ok(None)
        }
    }
}
