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
    /// The `handlers` slice contains layers that will register their stream
    /// responders with the connection's stream registry.
    pub fn websocket(
        socket: WebSocket,
        data: Resident<ConnectionData>,
        realm: RealmName,
        cluster_id: ClusterId,
        handlers: &[&dyn RegisterResponders],
    ) -> Arc<Self> {
        let (outgoing_tx, mut outgoing_rx) = tokio::sync::mpsc::channel::<Message>(32);
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

        // Spawn task that owns the actual WebSocket
        tokio::spawn(async move {
            let (mut ws_tx, mut ws_rx) = socket.split();

            loop {
                tokio::select! {
                    // Handle outgoing messages to websocket
                    Some(msg) = outgoing_rx.recv() => {
                        if ws_tx.send(msg).await.is_err() {
                            break;
                        }
                    }
                    // Handle outgoing stream messages
                    Some(msg) = stream_rx.recv() => {
                        let data = serde_cbor::to_vec(&msg).unwrap();
                        if ws_tx.send(Message::Binary(data.into())).await.is_err() {
                            break;
                        }
                    }
                    // Handle incoming messages from websocket
                    msg = ws_rx.next() => {
                        match msg {
                            Some(Ok(Message::Binary(data))) => {
                                if let Ok(message) = serde_cbor::from_slice::<StreamMessage>(&data) {
                                    streams_clone.dispatch(message).await;
                                }
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
        });

        Arc::new(Self {
            data,
            realm,
            cluster_id,
            cancel,
            streams,
        })
    }

    pub fn dtls() -> Arc<Self> {
        todo!()
    }
}
