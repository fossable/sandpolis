//! Client-side access to synced tunnel data.
//!
//! Tunnels are declared in realm config and bridged by the server; the client
//! only reads the replicated [`TunnelData`] rows to render the node-panel table
//! and the decorated world-view links.

use crate::TunnelData;
use anyhow::Result;
use native_model::Model;
use sandpolis_instance::InstanceId;

pub mod gui;

fn model_ids() -> [u32; 1] {
    [<TunnelData as Model>::native_model_id()]
}

/// Subscribe to live tunnel updates across the estate (call when a view opens).
pub fn subscribe() {
    sandpolis_client::sync::subscribe_all(model_ids(), None);
}

/// Every tunnel known to the client.
pub fn all_tunnels() -> Result<Vec<TunnelData>> {
    sandpolis_client::sync::scan_latest::<TunnelData>()
}

/// Tunnels that involve `instance`, as either endpoint.
pub fn query_tunnels(instance: InstanceId) -> Result<Vec<TunnelData>> {
    Ok(all_tunnels()?
        .into_iter()
        .filter(|t| t.listener_id == instance || t.terminator_id == instance)
        .collect())
}

/// Tunnels currently carrying (or ready to carry) traffic. Drives the link
/// decoration in the GUI.
pub fn active_tunnels() -> Result<Vec<TunnelData>> {
    Ok(all_tunnels()?
        .into_iter()
        .filter(|t| t.state.active())
        .collect())
}
