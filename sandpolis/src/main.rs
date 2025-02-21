use anyhow::bail;
use anyhow::Context;
use anyhow::Result;
use axum::routing::{any, get, post};
use axum::Router;
use clap::Parser;
use sandpolis::cli::CommandLine;
use sandpolis::cli::Commands;
use sandpolis::config::Configuration;
use sandpolis::InstanceState;
use sandpolis_database::Collection;
use sandpolis_database::Database;
use sandpolis_database::Document;
use sandpolis_group::{GroupCaCert, GroupData};
use sandpolis_network::ServerAddress;
use sandpolis_network::{ConnectionCooldown, ServerConnection};
use std::fs::File;
use std::io::Write;
use std::process::ExitCode;
use std::time::Duration;
use tokio::task::JoinSet;
use tracing::debug;
use tracing::info;
use tracing_subscriber::filter::LevelFilter;

pub mod routes;

#[tokio::main]
async fn main() -> Result<ExitCode> {
    #[cfg(all(
        not(feature = "server"),
        not(feature = "agent"),
        not(feature = "client")
    ))]
    bail!("No instance was enabled at build time");

    let args = CommandLine::parse();

    // Initialize logging for the instance
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::builder()
                .with_default_directive(if args.instance.trace {
                    LevelFilter::TRACE.into()
                } else if args.instance.debug {
                    LevelFilter::DEBUG.into()
                } else {
                    LevelFilter::INFO.into()
                })
                .from_env()?,
        )
        .init();

    info!(
        version = sandpolis::built_info::PKG_VERSION,
        build_time = sandpolis::built_info::BUILT_TIME_UTC,
        "Initializing Sandpolis"
    );

    // Load config
    let config = Configuration::new(&args)?;
    debug!(config = ?config, "Loaded configuration");

    // Get ready to do some cryptography
    rustls::crypto::aws_lc_rs::default_provider()
        .install_default()
        .expect("crypto provider is available");

    // Load instance database
    let db = Database::new(&config.database.storage)?;

    // Dispatch subcommand if one was given
    if let Some(command) = args.command {
        return Ok(command.dispatch()?);
    }

    // Load state
    let state = InstanceState::new(config.clone(), db).await?;

    // Prepare to launch instances
    let mut tasks = JoinSet::new();

    #[cfg(feature = "server")]
    {
        let s = state.clone();
        let c = config.clone();
        tasks.spawn(async move { server::main(c, s).await });
    }

    #[cfg(feature = "agent")]
    {
        let s = state.clone();
        let c = config.clone();
        tasks.spawn(async move { agent::main(c, s).await });
    }

    // The client must run on the main thread
    #[cfg(feature = "client")]
    {
        let app: Router<InstanceState> = Router::new().route("/versions", get(routes::versions));

        // Check command line preference
        #[cfg(all(feature = "client-gui", feature = "client-tui"))]
        {
            if args.client.gui {
                todo!();
            } else if args.client.tui {
                client::tui::main(config, state).await.unwrap();
            }
        }

        // #[cfg(feature = "client-tui")]
        // client::tui::main(config, state).await.unwrap();
    }

    // If this was a client, don't hold up the user by waiting for server/agent
    if !cfg!(feature = "client") {
        while let Some(result) = tasks.join_next().await {
            let _ = result??;
        }
    }

    Ok(ExitCode::SUCCESS)
}

#[cfg(feature = "server")]
pub mod server;

#[cfg(feature = "agent")]
pub mod agent;

#[cfg(feature = "client")]
pub mod client;
