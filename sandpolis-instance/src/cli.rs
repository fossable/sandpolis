use anyhow::{bail, Result};
use clap::{Parser, Subcommand};
use std::{path::PathBuf, str::FromStr};

fn parse_storage_dir(value: &str) -> Result<PathBuf> {
    let path = PathBuf::from_str(value)?;

    // If it's not a directory, create it
    if !std::fs::exists(&path)? {
        std::fs::create_dir_all(&path)?;
    }
    if !std::fs::metadata(&path)?.is_dir() {
        bail!("Storage directory must be a directory");
    }
    Ok(path)
}

fn default_storage_dir() -> PathBuf {
    "/tmp".into()
}

#[derive(Parser, Debug, Clone)]
pub struct InstanceCommandLine {
    /// Servers (address:port) to connect.
    ///
    /// For GS servers, connections will be established to all given values at
    /// the same time. For LS servers, agents, and clients, only one connection
    /// can be maintained at a time.
    #[clap(long)]
    pub server: Option<Vec<ServerAddress>>,

    /// Path to authentication certificate which will be installed into the database.
    /// Subsequent runs don't require this option.
    #[clap(long)]
    pub certificate: Option<String>,

    /// Enable debug mode
    #[clap(long, num_args = 0, default_value_t = false)]
    pub debug: bool,

    /// Enable trace mode
    #[clap(long, num_args = 0, default_value_t = false)]
    pub trace: bool,

    /// Storage directory
    #[clap(long, value_parser = parse_storage_dir, default_value = default_storage_dir().into_os_string())]
    pub storage: PathBuf,

    #[command(subcommand)]
    pub command: Option<InstanceCommands>,
}

#[derive(Subcommand, Debug, Clone)]
pub enum InstanceCommands {
    #[cfg(feature = "server")]
    /// Generate a new endpoint certificate signed by the group CA
    GenerateCert {
        /// Group to generate the certificate for
        #[clap(long, default_value = "default")]
        group: String,

        /// Output file path
        #[clap(long, default_value = "./endpoint.json")]
        output: PathBuf,
    },

    InstallCert {},
}
