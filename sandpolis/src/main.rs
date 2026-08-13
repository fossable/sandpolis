use anyhow::Result;
use clap::Parser;
use sandpolis::InstanceState;
use sandpolis::cli::CommandLine;
use sandpolis_instance::database::{DatabaseLayer, ScopeTable, WriteAuthority};
use sandpolis_instance::realm::Realms;
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

    #[allow(unused_mut)]
    let mut options = args.options();

    // The `.server` file is the whole trust bootstrap for an instance that
    // attaches to a server: it names the server, carries the realm CA, and
    // holds this instance's own certificate.
    #[allow(unused_variables)]
    let endpoint = sandpolis::load_server_file(&args)?;

    let stratum = match endpoint.as_ref() {
        Some((cert, _)) => sandpolis::ServerStratum::Local {
            global: cert.url()?,
        },
        None => sandpolis::ServerStratum::Global,
    };

    #[cfg(feature = "agent")]
    if let Some((cert, poll)) = endpoint.as_ref() {
        options.poll = poll.clone();
        options.servers.push(cert.url()?);
    }

    // Every realm this server serves comes from a `--realm` file. A zero-flag
    // run gets an implicit default realm so the all-in-one development loop
    // works without any setup; its CA lives only in the database.
    #[cfg(feature = "server")]
    let implicit_default_realm = stratum.is_global() && args.realm.is_empty();

    #[cfg(feature = "server")]
    if stratum.is_global() {
        for path in &args.realm {
            options
                .realms
                .push(sandpolis::config::RealmConfig::load(path)?);
        }
        if implicit_default_realm {
            info!(
                "Serving an implicit \"default\" realm. Pass `--realm default.realm` \
                 to keep its CA in a file that survives the database."
            );
        }
    }

    // Standalone subcommands (cert generation, version info, LSP) run without
    // starting any instances or opening a connection.
    if let Some(command) = args.command.as_ref()
        && command.standalone()
    {
        return args.command.unwrap().dispatch_standalone(&options).await;
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
    options
        .servers
        .push(format!("https://127.0.0.1:{}/default", options.listen.port()).parse()?);

    let database = DatabaseLayer::new(options.database.clone(), &sandpolis::MODELS, authority)?;

    // Realms are only ever created from files, so this is where the set is
    // fixed for the life of the process.
    #[allow(unused_mut)]
    let mut bootstraps = Vec::new();
    #[cfg(feature = "server")]
    if stratum.is_global() {
        if implicit_default_realm {
            bootstraps.push(sandpolis_instance::realm::config::RealmBootstrap::default());
        } else {
            for realm in &options.realms {
                bootstraps.push(realm.bootstrap()?);
            }
        }
    }

    let instance = sandpolis_instance::InstanceLayer::new(database.clone()).await?;

    #[allow(unused_mut)]
    let mut endpoint_certs = Vec::new();
    if let Some((cert, _)) = endpoint {
        endpoint_certs.push(cert);
    }

    let (realms, minted) = Realms::new(
        bootstraps,
        endpoint_certs,
        database.clone(),
        instance,
        cfg!(feature = "server") && stratum.is_global(),
        #[cfg(feature = "server")]
        options.listen.port(),
        #[cfg(not(feature = "server"))]
        0,
    )
    .await?;

    // A realm CA the server had to generate goes back into the file it came
    // from, which is the durable copy. The implicit default realm has no file,
    // so its CA stays in the database as before.
    #[cfg(feature = "server")]
    for ca in &minted {
        if let Some(realm) = options.realms.iter_mut().find(|r| r.name == ca.name) {
            realm.store_ca(ca.cert_pem.clone(), ca.key_pem.clone())?;
            info!(realm = %ca.name, path = ?realm.path(), "Wrote the realm CA back to its file");
        }
    }
    #[cfg(not(feature = "server"))]
    let _ = minted;

    // TODO do this somewhere else
    //
    // Realm files live on the global stratum server only, so it is also the only
    // instance that writes changes back to them.
    #[cfg(all(feature = "server", feature = "layer-account"))]
    if stratum.is_global() {
        let realms_config = options.realms.clone();
        sandpolis_account::set_account_persist(move |accounts| {
            // Accounts aren't per-realm yet, so they go back to the single
            // loaded realm. With several realms there's no way to tell which one
            // owns an account, so leave every file alone rather than guess.
            let mut realms_config = realms_config.clone();
            let accounts = accounts.to_vec();
            match realms_config.len() {
                1 => realms_config[0].modify(|c| {
                    // Only the account list is replaced; `account.scrape` and
                    // every other section keep whatever is on disk.
                    c.account.accounts = accounts.clone();
                    Ok(())
                }),
                0 => Ok(()),
                _ => {
                    tracing::warn!(
                        "Not persisting accounts: several realm files are loaded and \
                         accounts are not yet realm-scoped"
                    );
                    Ok(())
                }
            }
        });
    }

    // TODO do this somewhere else
    //
    // Only the global stratum server keeps the authoritative probe config; local
    // stratum servers don't persist a probe list of their own.
    #[cfg(all(feature = "server", feature = "layer-probe"))]
    if stratum.is_global() {
        let realms_config = options.realms.clone();
        sandpolis_probe::set_device_persist(move |devices| {
            let mut realms_config = realms_config.clone();
            let probe = sandpolis_probe::devices_to_config(devices);

            // A device records the server that reaches it, so route each one to
            // that server's realm. A device with no server can only be placed
            // when there's exactly one realm to place it in.
            let only_realm = realms_config.len() == 1;
            for realm in realms_config.iter_mut() {
                let name = realm.name.clone();
                let devices: Vec<_> = probe
                    .devices
                    .iter()
                    .filter(|device| match device.server.as_ref() {
                        Some(server) => server.realm == name,
                        None => only_realm,
                    })
                    .cloned()
                    .collect();
                realm.modify(|c| {
                    c.probe.devices = devices.clone();
                    Ok(())
                })?;
            }

            if !only_realm && probe.devices.iter().any(|device| device.server.is_none()) {
                tracing::warn!(
                    "Some probe devices name no server and several realm files are \
                     loaded, so they were not persisted to any of them"
                );
            }
            Ok(())
        });
    }

    // Load state
    let state = InstanceState::new(&options, database, realms, stratum.clone()).await?;

    info!(%stratum, "Starting Sandpolis");

    #[allow(unused_variables, unused_mut)]
    let mut tasks: JoinSet<Result<()>> = JoinSet::new();

    #[cfg(feature = "server")]
    {
        let s = state.clone();
        let o = options.clone();
        // On client builds nothing joins this set (the client owns the main
        // thread), so a server failure would otherwise be silent.
        tasks.spawn(async move {
            let result = sandpolis::server::main(o, s).await;
            if let Err(e) = &result {
                error!(error = %e, "Server task failed");
            }
            result
        });
    }

    // Auto-open a loopback connection from the co-located client to the local
    // server so it targets the local instance without configuration.
    #[cfg(all(feature = "server", feature = "client"))]
    sandpolis::client::spawn_local_server_connection(state.clone(), options.listen.port());

    // A standalone client is pointed at its server with `--server`.
    #[cfg(feature = "client")]
    if let Some((cert, _)) = sandpolis::load_server_file(&args)? {
        sandpolis::client::spawn_configured_server_connections(state.clone(), &[cert.url()?]);
    }

    #[cfg(feature = "agent")]
    {
        let s = state.clone();
        let o = options.clone();
        tasks.spawn(async move { sandpolis::agent::main(o, s).await });
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
                return args
                    .command
                    .unwrap()
                    .dispatch_client(&options, &state)
                    .await;
            }
            sandpolis::client::gui::main(options, state).await.unwrap();
            return Ok(ExitCode::SUCCESS);
        }
        #[cfg(target_os = "android")]
        {
            sandpolis::client::gui::main(options, state).await.unwrap();
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
