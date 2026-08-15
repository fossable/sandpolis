use super::{
    ConnectionData, InstanceConnection, RegisterResponders,
    stream::{StreamMessage, StreamRegistry},
};
use crate::ClusterId;
use crate::database::Resident;
use crate::realm::RealmName;
use axum::extract::ws::{Message, WebSocket};
use futures_util::{SinkExt, StreamExt};
use std::sync::Arc;
use tokio_util::sync::CancellationToken;

impl InstanceConnection {
    /// Wrap a websocket with an `InstanceConnection`.
    ///
    /// The `handlers` slice contains subsystems that will register their stream
    /// responders with the connection's stream registry.
    pub fn websocket(
        socket: WebSocket,
        data: Resident<ConnectionData>,
        realm: RealmName,
        cluster_id: ClusterId,
        handlers: &[&dyn RegisterResponders],
    ) -> Arc<Self> {
        let (_outgoing_tx, mut outgoing_rx) = tokio::sync::mpsc::channel::<Message>(32);
        let cancel = CancellationToken::new();
        let cancel_clone = cancel.clone();

        // Channel for the StreamRegistry to send outgoing StreamMessages
        let (stream_tx, mut stream_rx) = tokio::sync::mpsc::channel::<StreamMessage>(32);
        let streams = Arc::new(StreamRegistry::new(stream_tx));
        let streams_clone = streams.clone();

        // Register responders from all handlers
        for handler in handlers {
            handler.register_responders(&streams);
        }

        // The socket task times its own keepalive, so it writes the connection
        // row the same as the `InstanceConnection` it belongs to.
        let mut latency = super::LatencyProbe::new(data.clone());

        // Spawn task that owns the actual WebSocket
        tokio::spawn(async move {
            let (mut ws_tx, mut ws_rx) = socket.split();

            let mut keepalive = tokio::time::interval(super::KEEPALIVE_INTERVAL);
            keepalive.set_missed_tick_behavior(tokio::time::MissedTickBehavior::Delay);
            let mut last_frame = tokio::time::Instant::now();

            loop {
                tokio::select! {
                    // Prove the peer is still there. Tungstenite answers a ping
                    // itself, so any frame arriving is enough to keep this from
                    // firing — no cooperation needed from the other end.
                    _ = keepalive.tick() => {
                        if last_frame.elapsed() > super::KEEPALIVE_DEADLINE {
                            break;
                        }
                        if ws_tx.send(Message::Ping(latency.ping().into())).await.is_err() {
                            break;
                        }
                    }
                    // Handle outgoing messages to websocket
                    Some(msg) = outgoing_rx.recv() => {
                        if ws_tx.send(msg).await.is_err() {
                            break;
                        }
                    }
                    // Handle outgoing stream messages
                    Some(msg) = stream_rx.recv() => {
                        let data = serde_cbor::to_vec(&msg).unwrap();
                        streams_clone.record_tx(msg.stream_id, data.len() as u64);
                        if ws_tx.send(Message::Binary(data.into())).await.is_err() {
                            break;
                        }
                    }
                    // Handle incoming messages from websocket
                    msg = ws_rx.next() => {
                        // Any frame at all, including the pong answering our
                        // ping, says the peer is alive.
                        last_frame = tokio::time::Instant::now();
                        match msg {
                            Some(Ok(Message::Binary(data))) => {
                                if let Ok(message) = serde_cbor::from_slice::<StreamMessage>(&data) {
                                    streams_clone.record_rx(message.stream_id, data.len() as u64);
                                    streams_clone.dispatch(message).await;
                                }
                            }
                            // Tungstenite queues a pong itself, but only flushes
                            // it on the next write; answering here guarantees
                            // one, which is what the peer's deadline is waiting
                            // for.
                            Some(Ok(Message::Ping(payload))) => {
                                if ws_tx.send(Message::Pong(payload)).await.is_err() {
                                    break;
                                }
                            }
                            Some(Ok(Message::Pong(payload))) => {
                                latency.pong(&payload);
                            }
                            Some(Ok(_)) => {}
                            Some(Err(_)) | None => break,
                        }
                    }
                    // Handle cancellation
                    _ = cancel_clone.cancelled() => {
                        break;
                    }
                }
            }

            // However the socket ended, the connection is over. Cancelling here
            // is what tells the rest of the process: every `is_cancelled` check
            // and every janitor waiting on this token keys off it.
            cancel_clone.cancel();
        });

        Arc::new(Self {
            data,
            realm,
            cluster_id,
            cancel,
            streams,
            poll: Default::default(),
        })
    }
}
