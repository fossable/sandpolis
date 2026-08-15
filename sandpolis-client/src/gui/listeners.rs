use bevy::prelude::*;
use sandpolis_instance::InstanceId;
use sandpolis_instance::InstanceManager;
use sandpolis_instance::database::ResidentVecEvent;
use sandpolis_instance::network::NetworkManager;
use tokio::sync::mpsc;

/// Database update events from resident listeners.
#[derive(Clone, Debug)]
pub enum DatabaseUpdate {
    InstanceAdded(InstanceId),
    InstanceRemoved(InstanceId),
    /// A domain was created, deleted, or had its members changed.
    DomainsChanged,
    FilesystemChanged(InstanceId, std::path::PathBuf),
    NetworkTopologyChanged,
    InventoryUpdated(InstanceId),
    ShellOutput(String, Vec<u8>), // session_id, output
    PackagesChanged(InstanceId),
    DesktopEvent(InstanceId),
    TransferStarted(InstanceId, InstanceId, String), // from, to, filename
    TransferProgress(InstanceId, InstanceId, f32),
    TransferCompleted(InstanceId, InstanceId),
}

/// Resource containing channel receiver for database updates.
#[derive(Resource)]
pub struct DatabaseUpdateChannel {
    pub receiver: mpsc::UnboundedReceiver<DatabaseUpdate>,
}

/// Resource containing channel sender for database updates.
#[derive(Resource, Clone)]
pub struct DatabaseUpdateSender {
    pub sender: mpsc::UnboundedSender<DatabaseUpdate>,
}

/// Set up all resident listeners to forward database updates to Bevy
/// This runs in a background tokio task and sends updates through the channel
pub async fn setup_all_listeners(
    network: NetworkManager,
    instance: InstanceManager,
    tx: mpsc::UnboundedSender<DatabaseUpdate>,
) {
    // Listen for connection changes in the network manager
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

    // Identity rows arrive by replication after login and are what put agents on
    // the graph — the client holds no connection to any of them, so nothing else
    // would ever announce one.
    let tx_instances = tx.clone();
    instance.instances().listen(move |event| {
        if let ResidentVecEvent::Added(instance) = event {
            let instance_id = instance.read()._instance_id;
            let _ = tx_instances.send(DatabaseUpdate::InstanceAdded(instance_id));
            tracing::info!("Instance known: {}", instance_id);
        }
    });

    // Domains arrive by replication after login, so terrain membership has to be
    // resolved again whenever the set changes.
    let tx_domains = tx.clone();
    instance.domains().listen(move |_event| {
        let _ = tx_domains.send(DatabaseUpdate::DomainsChanged);
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
