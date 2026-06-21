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
    let instance_id = state.instance.instance_id;

    tokio::spawn(async move {
        loop {
            for connection in server.server_connections() {
                let has_ws = connection.inner.read().unwrap().is_some();
                if has_ws {
                    continue;
                }
                match connection.open_websocket(&network, instance_id).await {
                    Ok(ic) => {
                        sandpolis_client::sync::init(ic, database.clone());
                        tracing::info!("Established sync websocket to server");
                    }
                    Err(e) => {
                        tracing::debug!(error = %e, "Failed to open sync websocket");
                    }
                }
            }
            tokio::time::sleep(std::time::Duration::from_secs(5)).await;
        }
    });
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

    let server = state.server.clone();
    let url = match ServerUrl::from_str(&format!("https://127.0.0.1:{port}/default")) {
        Ok(url) => url,
        Err(e) => {
            tracing::error!(error = %e, "Failed to build local server URL");
            return;
        }
    };

    // Surface the local server in the (database-backed) saved server list so it
    // appears in the TUI, deduplicating so it isn't re-added every run.
    let already_saved = server
        .servers
        .iter()
        .any(|s| s.read().address == url);
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
            match server.connect(url.clone()).await {
                Ok(connection) => {
                    server
                        .outbound
                        .write()
                        .unwrap()
                        .push(std::sync::Arc::new(connection));
                    tracing::info!("Connected to local server");
                    return;
                }
                Err(e) => {
                    tracing::debug!(error = %e, "Local server not ready yet");
                    tokio::time::sleep(std::time::Duration::from_secs(2)).await;
                }
            }
        }
    });
}
