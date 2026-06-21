use anyhow::{Result, bail};
use clap::Parser;
use sandpolis::InstanceState;
use sandpolis::cli::CommandLine;
use sandpolis::config::Configuration;
use sandpolis_instance::database::DatabaseLayer;
use std::process::ExitCode;
use tokio::task::JoinSet;
use tracing::info;
use tracing_subscriber::filter::LevelFilter;

#[tokio::main]
async fn main() -> Result<ExitCode> {
    #[cfg(all(
        not(feature = "server"),
        not(feature = "agent"),
        not(feature = "client")
    ))]
    {
        bail!("No instance was enabled at build time");
    }

    #[allow(unreachable_code)]
    let args = CommandLine::parse();

    // A non-standalone subcommand opens a TUI (or prints JSON), so it owns the
    // terminal; send logs to a file in that case instead of corrupting the view.
    let use_log_file = matches!(&args.command, Some(c) if !c.standalone());

    // Initialize logging for the instance
    let level = if args.instance.trace {
        LevelFilter::TRACE
    } else if args.instance.debug {
        LevelFilter::DEBUG
    } else {
        LevelFilter::INFO
    };
    let make_filter = || {
        tracing_subscriber::EnvFilter::builder()
            .with_default_directive(level.into())
            .from_env()
    };
    if use_log_file {
        let file = std::fs::OpenOptions::new()
            .append(true)
            .create(true)
            .open("sandpolis.log")?;
        tracing_subscriber::fmt()
            .with_env_filter(make_filter()?)
            .with_writer(file)
            .init();
    } else {
        tracing_subscriber::fmt()
            .with_env_filter(make_filter()?)
            .init();
    }

    // Get ready to do some cryptography
    rustls::crypto::aws_lc_rs::default_provider()
        .install_default()
        .expect("crypto provider is available");

    // Only server instances accept a --config path; other instances use
    // $S7S_CONFIG or the default.
    #[allow(unused_mut)]
    let mut config_path: Option<std::path::PathBuf> = None;
    #[cfg(feature = "server")]
    {
        config_path = args.config.clone();
    }

    // Load config
    let mut config = Configuration::new(config_path)?;

    // Realm certs come from the command line only; they're loaded fresh on
    // every run and never persisted to the config or database.
    config.realm.realm_certs = args.realm.realm_cert.clone();

    // The database location comes from the command line, not the config file.
    config.database.storage = args.database.data_dir.clone();

    // A `--poll` flag selects polling mode for the agent, overriding config.
    #[cfg(feature = "agent")]
    if let Some(schedule) = args.agent.poll.clone() {
        config.agent.poll = Some(sandpolis_agent::config::PollConfig {
            schedule,
            timeout_secs: args.agent.poll_timeout.unwrap_or(30),
        });
    } else if let Some(timeout) = args.agent.poll_timeout {
        if let Some(poll) = config.agent.poll.as_mut() {
            poll.timeout_secs = timeout;
        }
    }

    // Standalone subcommands (cert generation, version info, LSP) run without
    // starting any instances or opening a connection.
    if let Some(command) = args.command.as_ref() {
        if command.standalone() {
            return args.command.unwrap().dispatch_standalone(&config).await;
        }
    }

    // TODO do this somewhere else
    #[cfg(all(feature = "server", feature = "layer-probe"))]
    {
        let base = config.clone();
        sandpolis_probe::set_device_persist(move |devices| {
            let mut cfg = base.clone();
            let probe = sandpolis_probe::devices_to_config(devices);
            cfg.modify(|c| {
                c.probe = probe.clone();
                Ok(())
            })
        });
    }

    // In an "all-in-one" run (the server runs in this same process), point the
    // co-located agent at the local server over loopback so no manual server
    // configuration is needed for local testing.
    #[cfg(all(feature = "server", feature = "agent"))]
    config.agent.servers.push(format!(
        "https://127.0.0.1:{}/default",
        config.server.listen.port()
    ));

    // Load state
    let state = InstanceState::new(
        config.clone(),
        DatabaseLayer::new(config.database.clone(), &sandpolis::MODELS)?,
    )
    .await?;

    info!("Starting Sandpolis");

    #[allow(unused_variables, unused_mut)]
    let mut tasks: JoinSet<Result<()>> = JoinSet::new();

    #[cfg(feature = "server")]
    {
        let s = state.clone();
        let c = config.clone();
        tasks.spawn(async move { sandpolis::server::main(c, s).await });
    }

    // Auto-open a loopback connection from the co-located client to the local
    // server so it targets the local instance without configuration.
    #[cfg(all(feature = "server", feature = "client"))]
    sandpolis::client::spawn_local_server_connection(state.clone(), config.server.listen.port());

    #[cfg(feature = "agent")]
    {
        let s = state.clone();
        let c = config.clone();
        tasks.spawn(async move { sandpolis::agent::main(c, s).await });
    }

    // The client runs on the main thread: bare invocation launches the GUI, a
    // subcommand opens a focused TUI or runs noninteractively.
    #[cfg(feature = "client")]
    {
        #[cfg(not(target_os = "android"))]
        {
            // Establish the sync websocket (the GUI does this itself).
            if args.command.is_some() {
                sandpolis::client::spawn_client_sync(state.clone());
                let target = args.target.clone();
                return args
                    .command
                    .unwrap()
                    .dispatch_client(&config, &state, target)
                    .await;
            }
            sandpolis::client::gui::main(config, state).await.unwrap();
            return Ok(ExitCode::SUCCESS);
        }
        #[cfg(target_os = "android")]
        {
            sandpolis::client::gui::main(config, state).await.unwrap();
            return Ok(ExitCode::SUCCESS);
        }
    }

    // No client: run as a daemon until the server/agent tasks finish.
    #[cfg(not(feature = "client"))]
    while let Some(result) = tasks.join_next().await {
        result??;
    }

    // Unreachable on client builds (the block above always returns).
    #[allow(unreachable_code)]
    Ok(ExitCode::SUCCESS)
}
