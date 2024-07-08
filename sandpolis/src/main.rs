use anyhow::Result;
use clap::Parser;
use futures::future::join_all;
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

    let mut futures = Vec::with_capacity(3);

    #[cfg(feature = "server")]
    futures.push(sandpolis::server::main());
    #[cfg(feature = "agent")]
    futures.push(sandpolis::agent::main());
    #[cfg(feature = "client")]
    futures.push(sandpolis::client::main());

    for result in join_all(futures).await {
        result?;
    }

    Ok(ExitCode::SUCCESS)
}
