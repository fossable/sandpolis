use crate::{InstanceState, config::Configuration};
use anyhow::Result;
use chrono::Utc;
use sandpolis_instance::network::RetryWait;
use sandpolis_server::{ServerConnectStrategy, ServerUrl};
use std::str::FromStr;
use std::sync::Arc;
use std::time::Duration;
use tokio::time::sleep;
use tracing::{debug, info, warn};

pub async fn main(config: Configuration, state: InstanceState) -> Result<()> {
    // Collect server URLs from agent config, plus optional comma-separated
    // override in $S7S_SERVER.
    let mut raw: Vec<String> = config.agent.servers.clone();
    if let Ok(env) = std::env::var("S7S_SERVER") {
        raw.extend(env.split(',').map(|s| s.trim().to_string()));
    }

    let urls: Vec<ServerUrl> = raw
        .iter()
        .filter(|s| !s.is_empty())
        .filter_map(|s| match ServerUrl::from_str(s) {
            Ok(url) => Some(url),
            Err(e) => {
                warn!(url = %s, error = %e, "Ignoring unparseable server URL");
                None
            }
        })
        .collect();

    if urls.is_empty() {
        warn!("Agent has no configured servers; idling");
        std::future::pending::<()>().await;
        return Ok(());
    }

    let mut tasks = tokio::task::JoinSet::new();

    // Periodically refresh the systemd collector. Its updates land in the local
    // database, which the SyncResponder streams to the server on demand.
    #[cfg(feature = "layer-health")]
    {
        let health = state.health.clone();
        tasks.spawn(async move {
            loop {
                {
                    let mut collector = health.systemd.lock().await;
                    if let Err(e) = sandpolis_agent::Collector::refresh(&mut *collector).await {
                        debug!(error = %e, "Failed to refresh systemd units");
                    }
                }
                sleep(std::time::Duration::from_secs(30)).await;
            }
        });
    }

    // Periodically refresh the inventory collectors (memory, users, packages).
    // Their updates land in the local database and sync to the server on demand.
    #[cfg(feature = "layer-inventory")]
    {
        let inventory = state.inventory.clone();
        tasks.spawn(async move {
            let mut ticks: u64 = 0;
            loop {
                if let Err(e) =
                    sandpolis_agent::Collector::refresh(&mut *inventory.memory.lock().await).await
                {
                    debug!(error = %e, "Failed to refresh memory info");
                }
                if let Err(e) =
                    sandpolis_agent::Collector::refresh(&mut *inventory.users.lock().await).await
                {
                    debug!(error = %e, "Failed to refresh users");
                }
                // Packages change rarely and are expensive to enumerate, so
                // refresh them every tenth tick (~5 minutes).
                if ticks % 10 == 0 {
                    if let Err(e) =
                        sandpolis_agent::Collector::refresh(&mut *inventory.packages.lock().await)
                            .await
                    {
                        debug!(error = %e, "Failed to refresh packages");
                    }
                }
                ticks = ticks.wrapping_add(1);
                sleep(std::time::Duration::from_secs(30)).await;
            }
        });
    }

    // Pick the connection strategy from config: a `poll` schedule selects
    // polling mode (periodic check-ins), otherwise the agent stays continuously
    // connected.
    let strategy = match &config.agent.poll {
        Some(poll) => {
            match ServerConnectStrategy::polling(&poll.schedule, Duration::from_secs(poll.timeout_secs))
            {
                Ok(strategy) => strategy,
                Err(e) => {
                    warn!(error = %e, "Invalid poll schedule; using continuous mode");
                    ServerConnectStrategy::Continuous
                }
            }
        }
        None => ServerConnectStrategy::Continuous,
    };

    for url in urls {
        let server = state.server.clone();
        let network = state.network.clone();
        let instance_id = state.instance.instance_id;
        let strategy = strategy.clone();
        tasks.spawn(async move {
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
                                if let Err(e) = entry.open_websocket(&network, instance_id).await {
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

                        match entry.open_websocket(&network, instance_id).await {
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
        });
    }

    while let Some(result) = tasks.join_next().await {
        result?;
    }
    Ok(())
}
