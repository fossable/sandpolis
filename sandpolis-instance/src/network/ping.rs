use crate::network::stream::Stream;
use crate::network::stream::{StreamRequester, StreamResponder};
use anyhow::Result;
use chrono::{DateTime, Utc};
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
    /// Every ping sent, when it went out, and its round trip in milliseconds
    /// once the matching pong comes back.
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
        let sent = (initial.ping, Utc::now(), None);
        tx.send(initial).await?;
        Ok(Self {
            results: RwLock::new(vec![sent]),
        })
    }

    async fn on_message(&self, response: Self::In, _tx: Sender<Self::Out>) -> Result<()> {
        // Match the pong against the most recent ping still outstanding: the
        // responder echoes the value back, so anything else on the stream
        // belongs to a ping we've already timed.
        if let Some((_, sent, rtt)) = self
            .results
            .write()
            .await
            .iter_mut()
            .rev()
            .find(|(ping, _, rtt)| *ping == response.pong && rtt.is_none())
        {
            *rtt = Some((Utc::now() - *sent).num_microseconds().unwrap_or(0) as f32 / 1000.0);
        }
        Ok(())
    }
}
