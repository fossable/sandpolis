use anyhow::Result;
use clap::Parser;
use std::{net::SocketAddr, path::PathBuf, process::ExitCode};
use tracing::debug;

#[derive(Parser, Debug)]
#[clap(author, version, about, long_about = None)]
struct CommandLine {
    #[cfg(any(feature = "agent", feature = "client"))]
    pub server: Option<Vec<String>>,
}

#[tokio::main]
async fn main() -> Result<ExitCode> {
    let args = CommandLine::parse();
    tracing_subscriber::fmt()
        .with_env_filter(tracing_subscriber::EnvFilter::from_default_env())
        .init();

    #[cfg(feature = "server")]
    crate::server::main().await?;

    Ok(ExitCode::SUCCESS)
}
