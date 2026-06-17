use crate::InstanceState;

#[cfg(feature = "client-gui")]
pub mod gui;

#[cfg(feature = "client-tui")]
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
