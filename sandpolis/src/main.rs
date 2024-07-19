use anyhow::Result;
use clap::Parser;
use futures::{future::join_all, Future};
use sandpolis::CommandLine;
use std::{net::SocketAddr, path::PathBuf, pin::Pin, process::ExitCode};
use tokio::join;
use tracing::debug;

#[tokio::main]
async fn main() -> Result<ExitCode> {
    let args = CommandLine::parse();
    tracing_subscriber::fmt()
        .with_env_filter(tracing_subscriber::EnvFilter::from_default_env())
        .init();

    #[cfg(feature = "server")]
    let server_thread = {
        let args = args.clone();
        tokio::spawn(async move { sandpolis::server::main(args).await })
    };

    #[cfg(feature = "agent")]
    let agent_thread = {
        let args = args.clone();
        tokio::spawn(async move { sandpolis::agent::main(args).await })
    };

    // The client must run on the main thread per bevy requirements
    #[cfg(feature = "client")]
    sandpolis::client::main(args).await?;

    // TODO single join
    // TODO if not client
    #[cfg(feature = "server")]
    join!(server_thread).0??;
    #[cfg(feature = "agent")]
    join!(agent_thread).0??;

    Ok(ExitCode::SUCCESS)
}
