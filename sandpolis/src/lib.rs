use anyhow::bail;
use anyhow::Result;
use clap::builder::OsStr;
use clap::{Parser, Subcommand};
use std::{path::PathBuf, str::FromStr};

pub mod core;

/// Build info
pub mod built_info {
    include!(concat!(env!("OUT_DIR"), "/built.rs"));
}

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

#[cfg_attr(feature = "client", derive(bevy::prelude::Resource))]
#[derive(Parser, Debug, Clone)]
#[clap(author, version, about, long_about = None)]
pub struct CommandLine {
    #[cfg(feature = "server")]
    #[clap(flatten)]
    pub server_args: crate::server::ServerCommandLine,

    #[cfg(feature = "client")]
    #[clap(flatten)]
    pub client_args: crate::client::ClientCommandLine,

    #[cfg(feature = "agent")]
    #[clap(flatten)]
    pub agent_args: crate::agent::AgentCommandLine,

    /// Servers (address:port) to connect
    #[clap(long)]
    pub server: Option<Vec<String>>,

    /// Path to authentication certificate which will be installed into the database
    /// on first run. Subsequent runs don't require this option.
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
    command: Option<Commands>,
}

#[derive(Subcommand, Debug, Clone)]
pub enum Commands {
    #[cfg(feature = "server")]
    /// Generate a new endpoint certificate signed by the group CA
    GenerateCert {
        /// Group to generate the certificate for
        #[clap(long, default_value = "default")]
        group: String,

        /// Output file path
        #[clap(long, default_value = "./endpoint.pem")]
        output: PathBuf,
    },
}

#[cfg(feature = "agent")]
pub mod agent;

#[cfg(feature = "server")]
pub mod server;

#[cfg(feature = "client")]
pub mod client;
