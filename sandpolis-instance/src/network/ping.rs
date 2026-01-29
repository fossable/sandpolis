use crate::network::stream::Stream;
use crate::network::stream::{StreamRequester, StreamResponder};
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

/// Responds to incoming ping requests by echoing back the ping value.
pub struct PingStreamResponder;

impl Stream for PingStreamResponder {
    fn tag() -> u32 {
        0
    }
}

impl StreamResponder for PingStreamResponder {
    type In = PingStreamRequest;
    type Out = PingStreamResponse;

    async fn on_message(&self, request: Self::In, sender: Sender<Self::Out>) -> Result<()> {
        sender
            .send(PingStreamResponse { pong: request.ping })
            .await?;
        Ok(())
    }
}

/// Initiates ping requests and processes pong responses.
pub struct PingStreamRequester {
    interval: Duration,
    results: RwLock<Vec<(PingValue, DateTime<Utc>, Option<f32>)>>,
}

impl Stream for PingStreamRequester {
    fn tag() -> u32 {
        0
    }
}

impl StreamRequester for PingStreamRequester {
    type In = PingStreamResponse;
    type Out = PingStreamRequest;

    async fn new(initial: Self::Out, tx: Sender<Self::Out>) -> Result<Self> {
        tx.send(initial).await?;
        todo!()
    }

    async fn on_message(&self, request: Self::In, tx: Sender<Self::Out>) -> Result<()> {
        Ok(())
    }
}
