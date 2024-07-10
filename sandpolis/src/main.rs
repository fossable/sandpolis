use anyhow::Result;
use clap::Parser;
use futures::{future::join_all, Future};
use std::{net::SocketAddr, path::PathBuf, pin::Pin, process::ExitCode};
use tokio::join;
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
    let server_thread = tokio::spawn(async { sandpolis::server::main().await });
    #[cfg(feature = "agent")]
    let agent_thread = tokio::spawn(async { sandpolis::agent::main().await });

    // The client must run on the main thread
    #[cfg(feature = "client")]
    sandpolis::client::main().await?;

    // TODO single join
    // TODO if not client
    #[cfg(feature = "server")]
    join!(server_thread).0??;
    #[cfg(feature = "agent")]
    join!(agent_thread).0??;

    Ok(ExitCode::SUCCESS)
}
