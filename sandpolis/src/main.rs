use anyhow::Result;
use clap::Parser;
use sandpolis::InstanceState;
use sandpolis::cli::CommandLine;
use sandpolis::config::Configuration;
use sandpolis_instance::database::DatabaseLayer;
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
    let subscriber = tracing_subscriber::fmt().with_env_filter(
        tracing_subscriber::EnvFilter::builder()
            .with_default_directive(if args.instance.trace {
                LevelFilter::TRACE.into()
            } else if args.instance.debug {
                LevelFilter::DEBUG.into()
            } else {
                LevelFilter::INFO.into()
            })
            .from_env()?,
    );

    #[cfg(feature = "client-tui")]
    let subscriber = subscriber.with_writer(
        std::fs::OpenOptions::new()
            .append(true)
            .create(true)
            .open("sandpolis.log")?,
    );
    subscriber.init();

    // Get ready to do some cryptography
    rustls::crypto::aws_lc_rs::default_provider()
        .install_default()
        .expect("crypto provider is available");

    // Load config
    let config = Configuration::new(&args)?;
    debug!(config = ?config, "Instance configuration");

    // Default to all compiled instance types
    #[allow(unused_mut)]
    let mut run_instances = Vec::<&str>::from([
        #[cfg(feature = "server")]
        "server",
        #[cfg(feature = "client")]
        "client",
        #[cfg(feature = "agent")]
        "agent",
    ]);

    // Dispatch subcommand if one was given
    #[allow(unused_imports)]
    use sandpolis::cli::Commands;
    match args.command {
        #[cfg(feature = "agent")]
        #[cfg(any(feature = "server", feature = "client"))]
        Some(Commands::Agent) => run_instances = vec!["agent"],

        #[cfg(feature = "client")]
        #[cfg(any(feature = "agent", feature = "server"))]
        Some(Commands::Client) => run_instances = vec!["client"],

        #[cfg(feature = "server")]
        #[cfg(any(feature = "agent", feature = "client"))]
        Some(Commands::Server) => run_instances = vec!["server"],

        Some(command) => return command.dispatch(&config).await,
        None => (),
    }

    info!(
        version = sandpolis::built_info::PKG_VERSION,
        build_time = sandpolis::built_info::BUILT_TIME_UTC,
        "Starting Sandpolis {}",
        run_instances.join(" + ")
    );

    // Load state
    let state = InstanceState::new(
        config.clone(),
        DatabaseLayer::new(config.database.clone(), &sandpolis::MODELS)?,
    )
    .await?;

    let mut tasks: JoinSet<Result<()>> = JoinSet::new();

    #[cfg(feature = "server")]
    if run_instances.contains(&"server") {
        let s = state.clone();
        let c = config.clone();
        tasks.spawn(async move { sandpolis::server::main(c, s).await });
    }

    #[cfg(feature = "agent")]
    if run_instances.contains(&"agent") {
        let s = state.clone();
        let c = config.clone();
        tasks.spawn(async move { sandpolis::agent::main(c, s).await });
    }

    // The client must run on the main thread
    #[cfg(feature = "client")]
    if run_instances.contains(&"client") {
        #[cfg(not(any(feature = "client-gui", feature = "client-tui")))]
        compile_error!("Missing client-gui or client-tui features");

        // Check command line preference if both are enabled
        #[cfg(all(feature = "client-gui", feature = "client-tui"))]
        {
            if args.client.gui {
                sandpolis::client::gui::main(config, state).await.unwrap();
            } else if args.client.tui {
                sandpolis::client::tui::main(config, state).await.unwrap();
            }
        }

        #[cfg(feature = "client-tui")]
        #[cfg(not(feature = "client-gui"))]
        sandpolis::client::tui::main(config, state).await.unwrap();

        #[cfg(feature = "client-gui")]
        #[cfg(not(feature = "client-tui"))]
        sandpolis::client::gui::main(config, state).await.unwrap();
    }

    // If this was a client, don't hold up the user by waiting for server/agent
    if !run_instances.contains(&"client") {
        while let Some(result) = tasks.join_next().await {
            result??;
        }
    }

    Ok(ExitCode::SUCCESS)
}
