use crate::{InstanceState, RuntimeOptions};
use anyhow::Result;
use chrono::Utc;
use sandpolis_instance::network::RetryWait;
use sandpolis_server::ServerConnectStrategy;
use std::sync::Arc;
use std::time::Duration;
use tokio::time::sleep;
use tracing::{debug, info, warn};

/// Bring up everything an agent needs and run it: the realm named by its
/// `.server` file, the database its collectors write into, and the layers over
/// both.
pub async fn start(args: crate::cli::AgentArgs) -> Result<std::process::ExitCode> {
    use sandpolis_instance::database::{DatabaseLayer, WriteAuthority};
    use sandpolis_instance::realm::Realms;

    let mut options = args.options();

    // The `.server` file is the whole connection policy: it names the server,
    // carries the realm CA, holds this agent's own certificate, and says whether
    // to stay connected or check in on a schedule.
    let mut endpoint_certs = Vec::new();
    if let Some((cert, poll)) = crate::load_server_file(args.server.as_deref())? {
        options.poll = poll;
        options.server = Some(cert.url()?);
        endpoint_certs.push(cert);
    }

    // An agent owns its own database outright; the server it attaches to
    // replicates from it.
    let database = DatabaseLayer::new(
        options.database.clone(),
        &crate::MODELS,
        WriteAuthority::Full,
    )?;

    // An agent knows exactly the realm its certificate names.
    let realms = Realms::for_client(endpoint_certs, database.clone())?;

    let state = InstanceState::new(
        &options,
        database,
        realms,
        // An agent runs no server of its own, so this is inert.
        crate::ServerStratum::Global,
    )
    .await?;

    info!("Starting Sandpolis agent");
    main(options, state).await?;
    Ok(std::process::ExitCode::SUCCESS)
}

pub async fn main(options: RuntimeOptions, state: InstanceState) -> Result<()> {
    // The agent's server comes from the `.server` file it was given
    // ($S7S_SERVER).
    let Some(url) = options.server.clone() else {
        warn!("Agent has no configured server; idling");
        std::future::pending::<()>().await;
        return Ok(());
    };

    // Every layer's collectors go on one runner, which owns their schedules and
    // lets a client enable or disable them individually. Their updates land in
    // the local database, which the SyncResponder streams to the server on
    // demand.
    let mut services = sandpolis_instance::service::ServiceRunner::new(
        state.instance.realm().clone(),
        state.instance.instance_id,
    );

    #[cfg(feature = "health")]
    state.health.register_services(&mut services);

    #[cfg(feature = "inventory")]
    state.inventory.register_services(&mut services);

    services.start()?;

    // Pick the connection strategy from the `.server` file: a `poll` schedule
    // selects polling mode (periodic check-ins), otherwise the agent stays
    // continuously connected.
    let strategy = match &options.poll {
        Some(poll) => {
            match ServerConnectStrategy::polling(
                &poll.schedule,
                Duration::from_secs(poll.timeout_secs),
            ) {
                Ok(strategy) => strategy,
                Err(e) => {
                    warn!(error = %e, "Invalid poll schedule; using continuous mode");
                    ServerConnectStrategy::Continuous
                }
            }
        }
        None => ServerConnectStrategy::Continuous,
    };

    let server = state.server.clone();
    let network = state.network.clone();
    let instance = state.instance.clone();

    match &strategy {
        // Hold a single connection open for the agent's lifetime,
        // reconnecting whenever it drops.
        ServerConnectStrategy::Continuous => {
            let mut retry = RetryWait::default();
            loop {
                match server.connect(url.clone()).await {
                    Ok(connection) => {
                        info!(url = %url, "Connected to server");
                        let cancel = connection.cancel.clone();
                        let entry = Arc::new(connection);
                        server.outbound.write().unwrap().push(entry.clone());

                        // Establish the websocket so the server can sync
                        // our database.
                        if let Err(e) = entry.open_websocket(&network, &instance).await {
                            warn!(error = %e, url = %url, "Failed to open websocket");
                        }
                        retry = RetryWait::default();
                        // When the connection is cancelled (e.g. dropped
                        // server-side), fall through and reconnect.
                        cancel.cancelled().await;
                        server
                            .outbound
                            .write()
                            .unwrap()
                            .retain(|c| !Arc::ptr_eq(c, &entry));
                        warn!(url = %url, "Server connection cancelled, reconnecting");
                    }
                    Err(e) => {
                        let wait = retry.next().unwrap();
                        debug!(error = %e, url = %url, waiting = ?wait, "Connection attempt failed");
                        sleep(wait).await;
                    }
                }
            }
        }

        // Stay disconnected between check-ins. On each scheduled tick,
        // open the websocket so the server pulls our accumulated data
        // and delivers any pending work, hold it briefly, then close.
        ServerConnectStrategy::Polling { schedule, timeout } => {
            // Build the connection (http client + banner) once and reuse
            // it; only the websocket opens and closes per window.
            let mut retry = RetryWait::default();
            let entry = loop {
                match server
                    .connect_with_strategy(url.clone(), strategy.clone())
                    .await
                {
                    Ok(connection) => break Arc::new(connection),
                    Err(e) => {
                        let wait = retry.next().unwrap();
                        debug!(error = %e, url = %url, waiting = ?wait, "Connection attempt failed");
                        sleep(wait).await;
                    }
                }
            };
            server.outbound.write().unwrap().push(entry.clone());
            info!(url = %url, schedule = %schedule, "Agent connected in polling mode");

            loop {
                // Sleep until the next scheduled check-in.
                let wait = schedule
                    .upcoming(Utc)
                    .next()
                    .and_then(|t| (t - Utc::now()).to_std().ok())
                    .unwrap_or_else(|| Duration::from_secs(1));
                debug!(url = %url, waiting = ?wait, "Waiting for next poll window");
                sleep(wait).await;

                match entry.open_websocket(&network, &instance).await {
                    Ok(_) => {
                        info!(url = %url, timeout = ?timeout, "Poll window open");
                        sleep(*timeout).await;
                        entry.close_websocket(&network);
                        debug!(url = %url, "Poll window closed");
                    }
                    Err(e) => {
                        warn!(error = %e, url = %url, "Poll check-in failed");
                    }
                }
            }
        }
    }
}
