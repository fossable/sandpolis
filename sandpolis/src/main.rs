use anyhow::bail;
use anyhow::Result;
use clap::Parser;
use futures::Future;
use sandpolis::CommandLine;
use std::process::ExitCode;
use tracing::info;

#[tokio::main]
async fn main() -> Result<ExitCode> {
    let args = CommandLine::parse();
    tracing_subscriber::fmt()
        .with_env_filter(tracing_subscriber::EnvFilter::from_default_env())
        .init();

    info!(
        version = sandpolis::built_info::PKG_VERSION,
        build_time = sandpolis::built_info::BUILT_TIME_UTC,
        "Initializing Sandpolis"
    );

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
    tokio::join!(server_thread).0??;
    #[cfg(feature = "agent")]
    tokio::join!(agent_thread).0??;

    bail!("No instance was enabled at build time")
}
