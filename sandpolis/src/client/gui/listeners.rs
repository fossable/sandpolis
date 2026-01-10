use super::components::DatabaseUpdate;
use crate::InstanceState;
use tokio::sync::mpsc;

/// Set up all resident listeners to forward database updates to Bevy
/// This runs in a background tokio task and sends updates through the channel
pub async fn setup_all_listeners(_state: InstanceState, tx: mpsc::UnboundedSender<DatabaseUpdate>) {
    // TODO: Subscribe to each layer's resident and forward updates

    // Example pattern for each layer:
    //
    // Filesystem layer:
    // if let Some(filesystem) = &state.filesystem {
    //     let mut listener = filesystem.resident.subscribe().await;
    //     let tx_clone = tx.clone();
    //     tokio::spawn(async move {
    //         while let Ok(update) = listener.recv().await {
    //             // Parse update and send to Bevy
    //             tx_clone.send(DatabaseUpdate::FilesystemChanged(instance_id, path)).ok();
    //         }
    //     });
    // }
    //
    // Network layer:
    // let mut listener = state.network.resident.subscribe().await;
    // let tx_clone = tx.clone();
    // tokio::spawn(async move {
    //     while let Ok(update) = listener.recv().await {
    //         tx_clone.send(DatabaseUpdate::NetworkTopologyChanged).ok();
    //     }
    // });
    //
    // Inventory layer:
    // if let Some(inventory) = &state.inventory {
    //     let mut listener = inventory.resident.subscribe().await;
    //     let tx_clone = tx.clone();
    //     tokio::spawn(async move {
    //         while let Ok(update) = listener.recv().await {
    //             tx_clone.send(DatabaseUpdate::InventoryUpdated(instance_id)).ok();
    //         }
    //     });
    // }
    //
    // Shell layer:
    // if let Some(shell) = &state.shell {
    //     let mut listener = shell.resident.subscribe().await;
    //     let tx_clone = tx.clone();
    //     tokio::spawn(async move {
    //         while let Ok(update) = listener.recv().await {
    //             // Parse shell output event
    //             tx_clone.send(DatabaseUpdate::ShellOutput(session_id, output)).ok();
    //         }
    //     });
    // }

    // Keep this task alive
    // In a real implementation, we'd have multiple spawned tasks listening to different residents
    // For now, just keep the channel alive
    let _ = tx;
}
