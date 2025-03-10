use anyhow::Result;
use axum::Router;
use axum::routing::{any, get, post};
use clap::Parser;
use colored::Colorize;
use sandpolis::InstanceState;
use sandpolis::cli::CommandLine;
use sandpolis::config::Configuration;
use sandpolis_database::Database;
use sandpolis_instance::InstanceType;
use std::process::ExitCode;
use tokio::task::JoinSet;
use tracing::debug;
use tracing::info;
use tracing_subscriber::filter::LevelFilter;

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

    // Load config
    let config = Configuration::new(&args)?;
    debug!(config = ?config, "Loaded configuration");

    // Dispatch subcommand if one was given
    if let Some(command) = args.command {
        return Ok(command.dispatch(&config)?);
    }

    info!(
        version = sandpolis::built_info::PKG_VERSION,
        build_time = sandpolis::built_info::BUILT_TIME_UTC,
        "Starting Sandpolis {}",
        Vec::<&str>::from([
            #[cfg(feature = "server")]
            &*"server".color(InstanceType::Server.color()),
            #[cfg(feature = "client")]
            &*"client".color(InstanceType::Client.color()),
            #[cfg(feature = "agent")]
            &*"agent".color(InstanceType::Agent.color()),
        ])
        .join(" + ")
    );

    // Get ready to do some cryptography
    rustls::crypto::aws_lc_rs::default_provider()
        .install_default()
        .expect("crypto provider is available");

    // Load instance database
    let db = Database::new(&config.database.storage)?;

    // Load state
    let state = InstanceState::new(config.clone(), db).await?;

    let mut tasks: JoinSet<Result<()>> = JoinSet::new();

    #[cfg(feature = "server")]
    {
        let s = state.clone();
        let c = config.clone();
        tasks.spawn(async move { sandpolis::server::main(c, s).await });
    }

    #[cfg(feature = "agent")]
    {
        let s = state.clone();
        let c = config.clone();
        tasks.spawn(async move { sandpolis::agent::main(c, s).await });
    }

    // The client must run on the main thread
    #[cfg(feature = "client")]
    {
        let app: Router<InstanceState> =
            Router::new().route("/versions", get(sandpolis::routes::versions));

        // Check command line preference if both are enabled
        #[cfg(all(feature = "client-gui", feature = "client-tui"))]
        {
            if args.client.gui {
                todo!();
            } else if args.client.tui {
                sandpolis::client::tui::main(config, state).await.unwrap();
            }
        }

        #[cfg(feature = "client-tui")]
        #[cfg(not(feature = "client-gui"))]
        sandpolis::client::tui::main(config, state).await.unwrap();
    }

    // If this was a client, don't hold up the user by waiting for server/agent
    #[cfg(not(feature = "client"))]
    {
        while let Some(result) = tasks.join_next().await {
            let _ = result?;
        }
    }

    Ok(ExitCode::SUCCESS)
}
