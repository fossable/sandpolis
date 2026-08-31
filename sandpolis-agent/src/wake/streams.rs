use crate::wake::WakeAction;
use sandpolis_macros::Stream;
use serde::Deserialize;
use serde::Serialize;

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
#[derive(Stream)]
pub struct WakeStreamResponder;

#[cfg(feature = "agent")]
impl sandpolis_instance::network::StreamResponder for WakeStreamResponder {
    type In = WakeStreamRequest;
    type Out = WakeStreamResponse;

    async fn on_message(
        &self,
        request: Self::In,
        sender: tokio::sync::mpsc::Sender<Self::Out>,
    ) -> anyhow::Result<()> {
        // Scheduled operations are not yet supported.
        if request.schedule.is_some() {
            sender
                .send(WakeStreamResponse::Failed(
                    "scheduled power operations are not supported".to_string(),
                ))
                .await?;
            return Ok(());
        }

        let response = match crate::wake::change_power_state(&request.action).await {
            Ok(()) => WakeStreamResponse::Ok,
            Err(e) => WakeStreamResponse::Failed(e.to_string()),
        };
        sender.send(response).await?;
        Ok(())
    }
}

#[cfg(feature = "agent")]
pub struct WakeResponderRegistration;

#[cfg(feature = "agent")]
impl sandpolis_instance::network::RegisterResponders for WakeResponderRegistration {
    fn register_responders(&self, registry: &sandpolis_instance::network::StreamRegistry) {
        registry.register_responder(|| WakeStreamResponder);
    }
}

#[cfg(feature = "agent")]
inventory::submit!(sandpolis_instance::network::ResponderRegistration(
    &WakeResponderRegistration
));
