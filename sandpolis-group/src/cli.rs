use anyhow::Result;
use clap::Parser;
use std::path::PathBuf;

use crate::GroupClientCert;

#[derive(Parser, Debug, Clone)]
pub struct GroupCommandLine {
    /// Path to authentication certificate which will be installed into the database.
    /// Subsequent runs don't require this option.
    #[clap(long)]
    certificate: Option<PathBuf>,
}

impl GroupCommandLine {
    pub fn certificate(&self) -> Result<Option<GroupClientCert>> {
        if let Some(cert) = self.certificate.as_ref() {
            let cert = std::fs::read(cert)?;
            Ok(serde_json::from_slice(&cert)?)
        } else {
            Ok(None)
        }
    }
}
