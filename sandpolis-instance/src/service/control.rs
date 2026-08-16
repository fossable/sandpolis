//! The service-control stream: enable, disable, and prod services.
//!
//! Carries writes only. The read path is the database sync engine: the hosting
//! instance writes [`ServiceData`](super::ServiceData) into its realm and clients
//! subscribe to that model to receive a snapshot plus live updates.
//!
//! Both servers and agents answer this stream. A client reaches an agent's
//! services by addressing the stream to it, so the server relays; the `key` in a
//! request is always local to the target (`"{layer}/{name}"`).

use serde::{Deserialize, Serialize};

/// Requests from a client to an instance's service runner.
#[derive(Serialize, Deserialize, Debug)]
pub enum ServiceControlRequest {
    /// Enable or disable a service. The choice is persisted, so it survives a
    /// restart of the hosting instance.
    SetEnabled { key: String, enabled: bool },

    /// Ask a periodic service to run its next pass now rather than waiting out
    /// its interval.
    RunNow { key: String },
}

/// Responses from an instance's service runner.
#[derive(Serialize, Deserialize, Debug)]
pub enum ServiceControlResponse {
    /// The operation succeeded.
    Ok,
    /// The operation failed.
    Error(String),
}

#[cfg(any(feature = "server", feature = "agent"))]
mod responder {
    use super::*;
    use crate::network::{
        RegisterResponders, ResponderRegistration, StreamRegistry, StreamResponder,
    };
    use anyhow::Result;
    use sandpolis_macros::Stream;
    use tokio::sync::mpsc::Sender;

    /// Hosting-instance side of the control stream.
    #[derive(Stream, Default)]
    pub struct ServiceControlResponder;

    impl StreamResponder for ServiceControlResponder {
        type In = ServiceControlRequest;
        type Out = ServiceControlResponse;

        async fn on_message(&self, request: Self::In, sender: Sender<Self::Out>) -> Result<()> {
            let response = match handle(request) {
                Ok(()) => ServiceControlResponse::Ok,
                Err(e) => {
                    tracing::warn!(error = %e, "Service control request failed");
                    ServiceControlResponse::Error(e.to_string())
                }
            };
            sender.send(response).await?;
            Ok(())
        }
    }

    fn handle(request: ServiceControlRequest) -> Result<()> {
        let services = crate::service::handle();
        match request {
            ServiceControlRequest::SetEnabled { key, enabled } => {
                services.set_enabled(&key, enabled)
            }
            ServiceControlRequest::RunNow { key } => services.run_now(&key),
        }
    }

    /// Registers [`ServiceControlResponder`] on each connection.
    pub struct ServiceControlResponderRegistration;

    impl RegisterResponders for ServiceControlResponderRegistration {
        fn register_responders(&self, registry: &StreamRegistry) {
            registry.register_responder(ServiceControlResponder::default);
        }
    }

    inventory::submit!(ResponderRegistration(&ServiceControlResponderRegistration));
}

#[cfg(any(feature = "server", feature = "agent"))]
pub use responder::ServiceControlResponder;

// What a client must be granted to drive an instance's services.
inventory::submit! {
    crate::network::stream::StreamPermission::require(
        sandpolis_macros::stream_tag!(ServiceControl), "service:control")
}
