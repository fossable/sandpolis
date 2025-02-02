use anyhow::bail;
use anyhow::Result;
use clap::builder::OsStr;
use clap::{Parser, Subcommand};
use futures::Future;
use sandpolis::CommandLine;
use std::process::ExitCode;
use tokio::task::JoinSet;
use tracing::info;

#[derive(Parser, Debug, Clone)]
#[clap(author, version, about, long_about = None)]
struct CommandLine {
    #[cfg(feature = "server")]
    #[clap(flatten)]
    pub server_args: sandpolis_server::ServerCommandLine,

    #[cfg(feature = "client")]
    #[clap(flatten)]
    pub client_args: sandpolis_client::ClientCommandLine,

    #[cfg(feature = "agent")]
    #[clap(flatten)]
    pub agent_args: sandpolis_agent::AgentCommandLine,

    #[clap(flatten)]
    pub instance_args: sandpolis_instance::cli::InstanceCommandLine,
}

#[tokio::main]
async fn main() -> Result<ExitCode> {
    #[cfg(all(
        not(feature = "server"),
        not(feature = "agent"),
        not(feature = "client")
    ))]
    bail!("No instance was enabled at build time");

    let args = CommandLine::parse();
    tracing_subscriber::fmt()
        .with_env_filter(tracing_subscriber::EnvFilter::from_default_env())
        .init();

    info!(
        version = sandpolis::built_info::PKG_VERSION,
        build_time = sandpolis::built_info::BUILT_TIME_UTC,
        "Initializing Sandpolis"
    );

    // Get ready to do some cryptography
    rustls::crypto::aws_lc_rs::default_provider()
        .install_default()
        .expect("crypto provider is available");

    let mut tasks = JoinSet::new();

    #[cfg(feature = "server")]
    tasks.spawn(async move {
        sandpolis_server::main(sandpolis_server::ServerCommandLine::parse()).await
    });

    #[cfg(feature = "agent")]
    tasks.spawn(async move { sandpolis_agent::main(args).await });

    // The client must run on the main thread per bevy requirements
    #[cfg(feature = "client")]
    sandpolis::client::main(args).await?;

    // If this was a client, don't hold up the user by waiting for server/agent
    if !cfg!(feature = "client") {
        while let Some(result) = tasks.join_next().await {
            result?;
        }
    }

    Ok(ExitCode::SUCCESS)
}
