use anyhow::Result;
use clap::Parser;
use futures::{future::join_all, Future};
use std::{net::SocketAddr, path::PathBuf, pin::Pin, process::ExitCode};
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

    let mut futures: Vec<Pin<Box<dyn Future<Output = Result<()>>>>> = Vec::with_capacity(3);

    #[cfg(feature = "server")]
    futures.push(Box::pin(sandpolis::server::main()));
    #[cfg(feature = "agent")]
    futures.push(Box::pin(sandpolis::agent::main()));
    #[cfg(feature = "client")]
    futures.push(Box::pin(sandpolis::client::main()));

    for result in join_all(futures).await {
        result?;
    }

    Ok(ExitCode::SUCCESS)
}
