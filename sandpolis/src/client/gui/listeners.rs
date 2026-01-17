use super::components::DatabaseUpdate;
use sandpolis_network::NetworkLayer;
use tokio::sync::mpsc;
use sandpolis_database::ResidentVecEvent;

/// Set up all resident listeners to forward database updates to Bevy
/// This runs in a background tokio task and sends updates through the channel
pub async fn setup_all_listeners(network: NetworkLayer, tx: mpsc::UnboundedSender<DatabaseUpdate>) {
    // Listen for connection changes in the network layer
    // Each connection represents an instance in the network
    let tx_connections = tx.clone();
    network.connections.listen(move |event| {
        match event {
            ResidentVecEvent::Added(connection) => {
                let instance_id = connection.read().remote_instance;
                let _ = tx_connections.send(DatabaseUpdate::InstanceAdded(instance_id));
                tracing::info!("New instance connected: {}", instance_id);
            }
            ResidentVecEvent::Updated(_connection) => {
                // Connection updated, trigger network topology refresh
                let _ = tx_connections.send(DatabaseUpdate::NetworkTopologyChanged);
            }
            ResidentVecEvent::Removed(connection_id) => {
                // Connection removed - we need to look up which instance this was
                // For now, just trigger a topology update
                tracing::info!("Connection removed: {:?}", connection_id);
                let _ = tx_connections.send(DatabaseUpdate::NetworkTopologyChanged);
            }
        }
    });

    // TODO: Add listeners for other layer-specific updates
    // - Filesystem changes
    // - Inventory updates
    // - Shell session events

    // Keep the main task alive
    loop {
        tokio::time::sleep(tokio::time::Duration::from_secs(3600)).await;
    }
}
