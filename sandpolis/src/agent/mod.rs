use crate::{InstanceState, RuntimeOptions};
use anyhow::Result;
use chrono::Utc;
#[cfg(feature = "uki")]
use sandpolis_agent::bootagent::BootAgentState;
#[cfg(feature = "uki")]
use sandpolis_agent::bootagent::streams::{BootStreamRequest, BootStreamRequester};
use sandpolis_instance::network::RetryWait;
use sandpolis_server::ServerConnectStrategy;
use std::sync::Arc;
use std::time::Duration;
use tokio::time::sleep;
use tracing::{debug, info, warn};

#[cfg(feature = "uki")]
use tracing::error;

#[cfg(feature = "uki")]
pub mod boot;

/// Bring up everything an agent needs and run it: the realm named by its realm
/// cert, the database its collectors write into, and the subsystems over both.
#[cfg(not(target_os = "android"))]
pub async fn start(args: crate::cli::AgentArgs) -> Result<std::process::ExitCode> {
    let mut options = args.options();

    // The realm cert names the server, carries the realm CA, and holds this
    // agent's own certificate. How the agent connects to that server — straight
    // through or on a schedule — comes from the command line.
    let mut endpoint_certs = Vec::new();
    if let Some(cert) = crate::load_realm_cert(args.realm.as_deref())? {
        options.server = Some(cert.url()?);
        endpoint_certs.push(cert);
    }

    let state = crate::endpoint_state(&options, endpoint_certs).await?;

    // A UKI build is always a boot agent: the fullscreen chainloader UI takes
    // over the main thread (`#[tokio::main]`'s `block_on` runs `start` there),
    // while the regular agent continues on the runtime's workers.
    #[cfg(feature = "uki")]
    {
        let boot_state = Arc::new(BootAgentState::default());
        {
            let boot_state = boot_state.clone();
            tokio::spawn(async move {
                if let Err(e) = main(options, state, boot_state).await {
                    error!(error = %e, "Agent failed");
                }
            });
        }

        let handle = tokio::runtime::Handle::current();
        boot::run_boot_ui(boot_state, boot::chainload_entries(), move |entry| {
            boot::chainload(&handle, entry)
        })?;
        Ok(std::process::ExitCode::SUCCESS)
    }

    #[cfg(not(feature = "uki"))]
    {
        main(options, state).await?;
        Ok(std::process::ExitCode::SUCCESS)
    }
}

pub async fn main(
    options: RuntimeOptions,
    state: InstanceState,
    #[cfg(feature = "uki")] boot: Arc<BootAgentState>,
) -> Result<()> {
    let Some(url) = options.server.clone() else {
        warn!("Waiting for server configuration");
        std::future::pending::<()>().await;
        return Ok(());
    };

    info!("Starting Sandpolis agent");

    // Every subsystem's collectors go on one runner, which owns their schedules and
    // lets a client enable or disable them individually. Their updates land in
    // the local database, which the SyncResponder streams to the server on
    // demand.
    let mut services = sandpolis_instance::service::ServiceRunner::new(
        state.instance.realm().clone(),
        state.instance.instance_id,
    );

    #[cfg(feature = "health")]
    state.health.register_services(&mut services);

    #[cfg(feature = "audit")]
    state.audit.register_services(&mut services);

    #[cfg(feature = "inventory")]
    state.inventory.register_services(&mut services);

    #[cfg(feature = "desktop")]
    state.desktop.register_services(&mut services);

    services.start()?;

    // A boot agent stays continuously connected so the server can hold it;
    // `--poll` doesn't exist in UKI builds.
    #[cfg(feature = "uki")]
    let strategy = ServerConnectStrategy::Continuous;

    // Pick the connection strategy from `--poll`: a schedule selects polling
    // mode (periodic check-ins), otherwise the agent stays continuously
    // connected.
    #[cfg(not(feature = "uki"))]
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
                        let socket = match entry.open_websocket(&network, &instance).await {
                            Ok(socket) => Some(socket),
                            Err(e) => {
                                warn!(error = %e, url = %url, "Failed to open websocket");
                                None
                            }
                        };
                        retry = RetryWait::default();

                        // A boot agent announces itself so the server can
                        // place a boot hold (and later release it).
                        #[cfg(feature = "uki")]
                        {
                            *boot.server.lock().unwrap() =
                                Some(format!("{}:{}", url.host, url.port));
                            if let Some(socket) = &socket
                                && let Err(e) = socket
                                    .open_stream(
                                        BootStreamRequester {
                                            state: boot.clone(),
                                        },
                                        BootStreamRequest::Announce {
                                            agent: instance.instance_id,
                                        },
                                    )
                                    .await
                            {
                                warn!(error = %e, "Failed to open boot stream");
                            }
                        }

                        // Reconnect when either token fires: the socket's, which
                        // is what a server-side drop or a dead peer cancels, or
                        // the connection's own. Waiting on the latter alone would
                        // never wake, since this loop holds the `Arc` that would
                        // have to drop first.
                        match socket {
                            Some(socket) => {
                                tokio::select! {
                                    _ = socket.cancel.cancelled() => {}
                                    _ = cancel.cancelled() => {}
                                }
                            }
                            None => cancel.cancelled().await,
                        }
                        server
                            .outbound
                            .write()
                            .unwrap()
                            .retain(|c| !Arc::ptr_eq(c, &entry));
                        #[cfg(feature = "uki")]
                        {
                            *boot.server.lock().unwrap() = None;
                        }
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
                        entry.close_websocket();
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
