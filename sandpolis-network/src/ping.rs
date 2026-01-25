use crate::{InstanceConnection, NetworkLayer, RequestResult, StreamHandler};
use anyhow::Result;
use chrono::{DateTime, Duration, Utc};
use serde::{Deserialize, Serialize};
use tokio::sync::{RwLock, mpsc::Sender};

type PingValue = u16;

#[derive(Serialize, Deserialize)]
pub struct PingStreamRequest {
    ping: PingValue,
}

#[derive(Serialize, Deserialize)]
pub struct PingStreamResponse {
    pong: PingValue,
}

/// Run application-level pings.
// #[derive(StreamResponder)]
pub struct PingStreamResponder;

impl StreamHandler for PingStreamResponder {
    type In = PingStreamRequest;
    type Out = PingStreamResponse;

    async fn on_message(&self, request: Self::In, sender: Sender<Self::Out>) -> Result<()> {
        sender
            .send(PingStreamResponse { pong: request.ping })
            .await?;
        Ok(())
    }
}

// #[derive(StreamResponder)]
pub struct PingStreamRequester {
    interval: Duration,
    results: RwLock<Vec<(PingValue, DateTime<Utc>, Option<f32>)>>,
}

impl StreamHandler for PingStreamRequester {
    type In = PingStreamResponse;
    type Out = PingStreamRequest;

    async fn on_message(&self, request: Self::In, sender: Sender<Self::Out>) -> Result<()> {
        Ok(())
    }
}
