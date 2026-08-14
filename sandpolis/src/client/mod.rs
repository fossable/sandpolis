use crate::InstanceState;

#[cfg(feature = "client")]
pub mod gui;

#[cfg(all(feature = "client", not(target_os = "android")))]
pub mod tui;

/// Establish the websocket to the first available server and install it for DB
/// sync. Runs in the background until a server connection exists (the user logs
/// in), then opens the websocket once and hands it to `sandpolis_client::sync`.
pub fn spawn_client_sync(state: InstanceState) {
    let server = state.server.clone();
    let network = state.network.clone();
    let database = state.network.database.clone();
    let instance = state.instance.clone();

    // Start surfacing notifications before any of them can arrive. This is the
    // one path both the GUI and the subcommand TUIs take, so it covers every
    // way the client runs; without a GUI to toast into, delivery falls through
    // to the operating system.
    if let Err(e) = sandpolis_client::notification::watch(&database) {
        tracing::warn!(error = %e, "Notifications will not be surfaced");
    }

    tokio::spawn(async move {
        loop {
            let conns = server.server_connections();
            tracing::debug!(
                count = conns.len(),
                "spawn_client_sync: checking server connections"
            );
            for connection in conns {
                let has_ws = connection.inner.read().unwrap().is_some();
                tracing::debug!(has_ws, "spawn_client_sync: connection slot");
                if has_ws {
                    continue;
                }
                tracing::info!("spawn_client_sync: opening websocket");
                match connection.open_websocket(&network, &instance).await {
                    Ok(ic) => {
                        tracing::info!("Established sync websocket to server");
                        // Register every server for routing; the first also becomes
                        // the primary connection backing `sync::connection()`.
                        sandpolis_client::sync::register_connection(
                            connection.url.clone(),
                            ic.clone(),
                        );
                        sandpolis_client::sync::init(ic, database.clone());

                        // Domains group nodes in the world view, so they're
                        // needed for as long as the GUI runs. Subscribing is a
                        // no-op before `init`, hence here rather than at
                        // startup.
                        sandpolis_client::sync::subscribe(
                            sandpolis_instance::domain::domain_model_id(),
                            None,
                        );

                        // Notifications are wanted for as long as the client
                        // runs, not only while some view is open, so this
                        // subscription is standing rather than opened by a
                        // panel.
                        sandpolis_client::notification::subscribe();
                    }
                    Err(e) => {
                        tracing::info!(error = %e, "Failed to open sync websocket");
                    }
                }
            }
            tokio::time::sleep(std::time::Duration::from_millis(200)).await;
        }
    });
}

/// Connect to each server given on the command line with `--server`.
///
/// Clients read no config file, so this is how a standalone client is pointed at
/// a server without going through the GUI login dialog. Either stratum works —
/// the client addresses agents by id and the servers route to them.
pub fn spawn_configured_server_connections(
    state: InstanceState,
    urls: &[sandpolis_server::ServerUrl],
) {
    for url in urls {
        spawn_server_connection(state.clone(), url.clone());
    }
}

/// In an "all-in-one" build (a server is compiled and running in this same
/// process), automatically open a loopback connection to the local server so the
/// client targets it without any manual configuration. Retries until the
/// in-process server is listening, then registers the connection so
/// [`spawn_client_sync`] establishes the sync websocket.
#[cfg(feature = "server")]
pub fn spawn_local_server_connection(state: InstanceState, port: u16) {
    use sandpolis_server::ServerUrl;
    use std::str::FromStr;

    let url = match ServerUrl::from_str(&format!("https://127.0.0.1:{port}/default")) {
        Ok(url) => url,
        Err(e) => {
            tracing::error!(error = %e, "Failed to build local server URL");
            return;
        }
    };

    spawn_server_connection(state, url);
}

/// Open (and retain) a connection to `url`, retrying until it succeeds.
fn spawn_server_connection(state: InstanceState, url: sandpolis_server::ServerUrl) {
    let server = state.server.clone();

    // Surface the server in the (database-backed) saved server list so it
    // appears in the TUI, deduplicating so it isn't re-added every run. On a
    // read-only replica this list can't be written, which is harmless — the
    // connection itself still comes up.
    let already_saved = server.servers.iter().any(|s| s.read().address == url);
    if !already_saved {
        use sandpolis_instance::database::{DataCreation, DataIdentifier, DataRevision};
        if let Err(e) = server.save_server(sandpolis_server::client::SavedServerData {
            address: url.clone(),
            token: sandpolis_server::user::ClientAuthToken(String::new()),
            user: sandpolis_server::user::UserName::default(),
            _id: DataIdentifier::default(),
            _revision: DataRevision::Latest(0),
            _creation: DataCreation::default(),
        }) {
            tracing::debug!(error = %e, "Failed to save local server entry");
        }
    }

    tokio::spawn(async move {
        loop {
            tracing::debug!(%url, "Attempting server connection");
            match server.connect(url.clone()).await {
                Ok(connection) => {
                    server
                        .outbound
                        .write()
                        .unwrap()
                        .push(std::sync::Arc::new(connection));
                    tracing::info!(%url, "Connected to server");
                    return;
                }
                Err(e) => {
                    tracing::debug!(error = %e, %url, "Server not ready yet, retrying in 2s");
                    tokio::time::sleep(std::time::Duration::from_secs(2)).await;
                }
            }
        }
    });
}
