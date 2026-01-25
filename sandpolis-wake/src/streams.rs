use crate::WakeAction;
use crate::messages::{WakeRequest, WakeResponse};
use anyhow::Result;
use sandpolis_macros::StreamResponder;
use sandpolis_network::StreamHandler;
use serde::Deserialize;
use serde::Serialize;
use tokio::sync::mpsc::Sender;

/// Request that the agent alter its power state.
#[derive(Serialize, Deserialize)]
pub struct WakeStreamRequest {
    /// Type of power operation
    pub action: WakeAction,

    /// When to initiate the operation
    pub schedule: Option<String>,
}

#[derive(Serialize, Deserialize)]
pub enum WakeStreamResponse {
    Ok,
    Failed(String),
}

/// Modify the agent's current power state (shutdown, reboot, etc).
#[derive(StreamResponder)]
pub struct WakeStreamResponder;

impl StreamHandler for WakeStreamResponder {
    type In = WakeStreamRequest;
    type Out = WakeStreamResponse;

    async fn on_message(&self, request: Self::In, _sender: Sender<Self::Out>) -> Result<()> {
        // TODO: implement power state changes
        // libc::reboot
        todo!()
    }
}
