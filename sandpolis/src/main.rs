use anyhow::{Result, bail};
use clap::Parser;
use sandpolis::InstanceState;
use sandpolis::cli::CommandLine;
use sandpolis::config::Configuration;
use sandpolis_instance::database::{DatabaseLayer, ScopeTable, WriteAuthority};
use std::process::ExitCode;
use tokio::task::JoinSet;
use tracing::{error, info};
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

    let stratum = args.stratum();

    // Only the global stratum server reads a config file — it owns the
    // authoritative database, so it owns the authoritative settings. Local
    // stratum servers, agents and clients are configured entirely by flags.
    #[allow(unused_mut)]
    let mut config = if stratum.is_global() {
        #[cfg(feature = "server")]
        {
            Configuration::load_global(args.config.clone())?
        }
        #[cfg(not(feature = "server"))]
        {
            Configuration::default()
        }
    } else {
        Configuration::default()
    };

    // Realm certs come from the command line only; they're loaded fresh on
    // every run and never persisted to the config or database.
    config.realm.realm_certs = args.realm.realm_cert.clone();

    // The database location comes from the command line, not the config file.
    config.database.storage = args.database.data_dir.clone();
    if args.database.ephemeral {
        config.database.ephemeral = true;
    }

    #[cfg(feature = "server")]
    if let Some(listen) = args.listen {
        config.server.listen = listen;
    }

    #[cfg(feature = "client")]
    if let Some(fps) = args.client.fps {
        config.client.fps = fps;
    }

    // Servers to connect to. A local stratum server dials its global stratum
    // server (handled by `sandpolis::server`); agents and clients take theirs
    // from `--server`.
    #[cfg(feature = "agent")]
    if let Some(servers) = args.servers.server.as_ref() {
        config.agent.servers = servers.iter().map(|url| url.to_string()).collect();
    }

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
    //
    // Config lives on the global stratum server only, so it is also the only
    // instance that writes changes back to it.
    #[cfg(all(feature = "server", feature = "layer-account"))]
    if stratum.is_global() {
        let base = config.clone();
        sandpolis_account::set_account_persist(move |accounts| {
            let mut cfg = base.clone();
            let accounts = accounts.to_vec();
            // Only the account list is replaced; `account.scrape` and every
            // other section keep whatever is on disk.
            cfg.modify(|c| {
                c.account.accounts = accounts.clone();
                Ok(())
            })
        });
    }

    // TODO do this somewhere else
    //
    // Only the global stratum server keeps the authoritative probe config; local
    // stratum servers don't persist a probe list of their own.
    #[cfg(all(feature = "server", feature = "layer-probe"))]
    if stratum.is_global() {
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

    // A local stratum server holds per-instance write authority: it owns the
    // data of the instances directly connected to it (as granted by the global
    // stratum server) and replicates everything else. The global stratum
    // server, agents, and clients own their databases outright.
    let authority = if stratum.is_local() {
        WriteAuthority::Scoped(std::sync::Arc::new(ScopeTable::default()))
    } else {
        WriteAuthority::Full
    };

    // In an "all-in-one" run (the server runs in this same process), point the
    // co-located agent at the local server over loopback so no manual server
    // configuration is needed for local testing. This works in either stratum:
    // the co-located agent shares this process's InstanceId, and a server
    // always holds write authority for its own scope.
    #[cfg(all(feature = "server", feature = "agent"))]
    config.agent.servers.push(format!(
        "https://127.0.0.1:{}/default",
        config.server.listen.port()
    ));

    // Load state
    let state = InstanceState::new(
        config.clone(),
        DatabaseLayer::new(config.database.clone(), &sandpolis::MODELS, authority)?,
        stratum.clone(),
    )
    .await?;

    info!(%stratum, "Starting Sandpolis");

    #[allow(unused_variables, unused_mut)]
    let mut tasks: JoinSet<Result<()>> = JoinSet::new();

    #[cfg(feature = "server")]
    {
        let s = state.clone();
        let c = config.clone();
        // On client builds nothing joins this set (the client owns the main
        // thread), so a server failure would otherwise be silent.
        tasks.spawn(async move {
            let result = sandpolis::server::main(c, s).await;
            if let Err(e) = &result {
                error!(error = %e, "Server task failed");
            }
            result
        });
    }

    // Auto-open a loopback connection from the co-located client to the local
    // server so it targets the local instance without configuration.
    #[cfg(all(feature = "server", feature = "client"))]
    sandpolis::client::spawn_local_server_connection(state.clone(), config.server.listen.port());

    // A standalone client is pointed at its server(s) with `--server`.
    #[cfg(feature = "client")]
    if let Some(servers) = args.servers.server.as_ref() {
        sandpolis::client::spawn_configured_server_connections(state.clone(), servers);
    }

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
                return args.command.unwrap().dispatch_client(&config, &state).await;
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

    // Unreachable on client builds
    #[allow(unreachable_code)]
    Ok(ExitCode::SUCCESS)
}
