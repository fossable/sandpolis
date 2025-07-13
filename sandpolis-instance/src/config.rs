use anyhow::Result;
use sandpolis_core::LayerConfig;
use serde::{Deserialize, Serialize};
use std::path::PathBuf;
use tracing::debug;

use crate::cli::InstanceCommandLine;

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct InstanceConfig {
    /// Directory where the admin socket will be created
    pub socket_directory: Option<PathBuf>,
}

impl Default for InstanceConfig {
    fn default() -> Self {
        Self {
            socket_directory: Some("/tmp".into()),
        }
    }
}

impl InstanceConfig {
    pub fn clear_socket_path(&self, filename: &str) -> Result<()> {
        let socket_directory = self
            .socket_directory
            .as_ref()
            .expect("Socket directory is defined if you're calling me");

        // If the socket directory doesn't exist, create it
        if !std::fs::exists(&socket_directory)? {
            std::fs::create_dir_all(&socket_directory)?;
        }

        let socket = socket_directory.join(filename);

        // If the socket already exists, delete it
        if std::fs::exists(&socket)? {
            debug!(path = %socket.display(), "Removing existing socket");
            std::fs::remove_file(socket)?;
        }

        Ok(())
    }
}

impl LayerConfig<InstanceCommandLine> for InstanceConfig {
    fn override_cli(&mut self, args: &InstanceCommandLine) {
        if args.no_admin_socket {
            self.socket_directory = None;
        }
    }
}
