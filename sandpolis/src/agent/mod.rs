use crate::{InstanceState, config::Configuration};
use anyhow::Result;
use sandpolis_instance::network::RetryWait;
use sandpolis_server::ServerUrl;
use std::str::FromStr;
use std::sync::Arc;
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

    for url in urls {
        let server = state.server.clone();
        let network = state.network.clone();
        let instance_id = state.instance.instance_id;
        tasks.spawn(async move {
            let mut retry = RetryWait::default();
            loop {
                match server.connect(url.clone()).await {
                    Ok(connection) => {
                        info!(url = %url, "Connected to server");
                        let cancel = connection.cancel.clone();
                        let entry = Arc::new(connection);
                        server.outbound.write().unwrap().push(entry.clone());

                        // Establish the websocket so the server can sync our
                        // database.
                        if let Err(e) = entry.open_websocket(&network, instance_id).await {
                            warn!(error = %e, url = %url, "Failed to open websocket");
                        }
                        retry = RetryWait::default();
                        // Stay alive while the internal poll loop handles its
                        // own per-request retries. When the connection is
                        // cancelled (e.g. dropped server-side), fall through
                        // and attempt to reconnect.
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
        });
    }

    while let Some(result) = tasks.join_next().await {
        result?;
    }
    Ok(())
}
