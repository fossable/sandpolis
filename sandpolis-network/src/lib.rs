use anyhow::Result;
use anyhow::anyhow;
use axum::extract::ws::{Message, WebSocket};
use chrono::{DateTime, Utc};
use cron::Schedule;
use futures_util::{SinkExt, StreamExt};
use native_db::ToKey;
use native_model::Model;
use sandpolis_core::ClusterId;
use sandpolis_core::{InstanceId, RealmName};
use sandpolis_database::DatabaseLayer;
use sandpolis_database::Resident;
use sandpolis_database::ResidentVec;
use sandpolis_macros::data;
use serde::{Deserialize, Serialize};
use serde_with::chrono::serde::{ts_seconds, ts_seconds_option};
use std::collections::HashMap;
use std::fmt::Display;
use std::net::IpAddr;
use std::net::ToSocketAddrs;
use std::str::FromStr;
use std::sync::RwLock;
use std::{cmp::min, net::SocketAddr, sync::Arc, time::Duration};
use stream::{StreamHandler, StreamId, StreamMessage};
use tokio_util::sync::CancellationToken;
use tracing::debug;
use url::Url;

pub mod cli;
pub mod config;
pub mod messages;
pub mod routes;
pub mod stream;

#[cfg(feature = "server")]
pub mod server;

#[data]
#[derive(Default)]
pub struct NetworkLayerData {}

#[derive(Clone)]
#[cfg_attr(feature = "client-gui", derive(bevy::prelude::Resource))]
pub struct NetworkLayer {
    data: Resident<NetworkLayerData>,

    /// Inbound connections
    pub inbound: Arc<RwLock<Vec<Arc<InstanceConnection>>>>,

    /// All connections tracked in the database
    pub connections: ResidentVec<ConnectionData>,

    pub database: DatabaseLayer,
}

impl NetworkLayer {
    pub async fn new(database: DatabaseLayer) -> Result<Self> {
        debug!("Initializing network layer");

        let realm = database.realm(RealmName::default())?;
        let network = Self {
            inbound: Arc::new(RwLock::new(Vec::new())),
            data: realm.resident(())?,
            connections: realm.resident_vec(())?,
            database,
        };

        Ok(network)
    }
}

/// Convenience type to be used as return of request handler.
pub type RequestResult<T> = Result<axum::Json<T>, axum::Json<T>>;

#[data(temporal)]
#[derive(Default)]
pub struct ConnectionData {
    #[secondary_key]
    pub _instance_id: InstanceId,

    // TODO option before it's figured out
    pub remote_instance: InstanceId,

    /// Application-level bytes read since the connection was established
    pub read_bytes: u64,

    /// Total number of bytes written since the connection was established
    pub write_bytes: u64,

    /// "Recent" read throughput in bytes/second
    pub read_throughput: u64,

    /// "Recent" write throughput in bytes/second
    pub write_throughput: u64,

    pub local_socket: Option<SocketAddr>,
    pub remote_socket: Option<SocketAddr>,

    #[serde(with = "ts_seconds")]
    pub established: DateTime<Utc>,

    #[serde(with = "ts_seconds_option")]
    pub disconnected: Option<DateTime<Utc>>,
}

/// Connection to another instance that's suitable for running streams. The transport
/// will either be a Websocket (when a server is one of the peers) or DTLS (when
/// neither peer is a server).
///
/// For the DTLS case, reliable/orderly delivery is not guaranteed which fits
/// the use case of direct connections.
pub struct InstanceConnection {
    tx: tokio::sync::mpsc::Sender<Message>,
    pub data: Resident<ConnectionData>,
    pub realm: RealmName,
    pub cluster_id: ClusterId,
    pub cancel: CancellationToken,
    streams: Arc<RwLock<HashMap<StreamId, Arc<dyn StreamHandler>>>>,
}

impl Drop for InstanceConnection {
    fn drop(&mut self) {
        self.cancel.cancel();
    }
}

impl InstanceConnection {
    /// Wrap a websocket with an `InstanceConnection`.
    pub fn websocket(
        socket: WebSocket,
        data: Resident<ConnectionData>,
        realm: RealmName,
        cluster_id: ClusterId,
    ) -> Arc<Self> {
        let (outgoing_tx, mut outgoing_rx) = tokio::sync::mpsc::channel::<Message>(32);
        let cancel = CancellationToken::new();
        let cancel_clone = cancel.clone();

        let streams: Arc<RwLock<HashMap<StreamId, Arc<dyn StreamHandler>>>> =
            Arc::new(RwLock::new(HashMap::new()));
        let streams_clone = streams.clone();

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
                    // Handle incoming messages from websocket
                    msg = ws_rx.next() => {
                        match msg {
                            Some(Ok(Message::Binary(data))) => {
                                if let Ok(message) = serde_cbor::from_slice::<StreamMessage>(&data) {
                                    let streams = streams_clone.read().unwrap();
                                    if let Some(handler) = streams.get(&message.stream_id) {
                                        handler.on_receive(message);
                                    }
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
            tx: outgoing_tx,
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

    /// Register a stream handler and return a sender for outbound messages.
    pub fn register_stream<S: stream::Stream>(
        &self,
        handler: Arc<dyn StreamHandler>,
    ) -> (StreamId, tokio::sync::mpsc::Sender<StreamMessage>) {
        let id = S::generate_id();
        self.streams.write().unwrap().insert(id, handler);

        let tx = self.tx.clone();
        let (msg_tx, mut msg_rx) = tokio::sync::mpsc::channel::<StreamMessage>(32);

        tokio::spawn(async move {
            while let Some(msg) = msg_rx.recv().await {
                let data = serde_cbor::to_vec(&msg).unwrap();
                if tx.send(Message::Binary(data.into())).await.is_err() {
                    break;
                }
            }
        });

        (id, msg_tx)
    }

    /// Remove a stream (Drop handles cleanup on the handler).
    pub fn close_stream(&self, stream_id: StreamId) {
        self.streams.write().unwrap().remove(&stream_id);
    }

    /// Send a raw message to the remote peer.
    pub async fn send(&self, msg: Message) -> Result<()> {
        self.tx
            .send(msg)
            .await
            .map_err(|_| anyhow!("Connection closed"))
    }
}


/// How long to wait to retry after an unsuccessful connection attempt.
#[derive(Serialize, Deserialize, Clone, PartialEq, Debug)]
pub enum RetryWait {
    /// An exponentially increasing wait
    Exponential {
        /// Initial wait value
        initial: Duration,

        /// Number of retries required for the total wait to
        /// increase by a factor of the initial value.
        constant: f64,

        /// Maximum wait value
        limit: Option<Duration>,

        /// Number of times waited
        iteration: u32,
    },

    /// A wait period that never changes
    Constant {
        /// Initial wait value
        initial: Duration,

        /// Number of times waited
        iteration: u32,
    },
}

impl Iterator for RetryWait {
    type Item = Duration;

    fn next(&mut self) -> Option<Self::Item> {
        match self {
            RetryWait::Exponential {
                initial,
                constant,
                limit,
                iteration,
            } => {
                let value = Duration::from_millis(
                    ((initial.as_millis() as f64)
                        * (initial.as_millis() as f64).powf(*iteration as f64 / *constant))
                        as u64,
                );

                *iteration += 1;

                Some(match limit {
                    // Apply maximum limit
                    Some(l) => min(value, *l),
                    None => value,
                })
            }
            RetryWait::Constant { initial, iteration } => {
                *iteration += 1;
                Some(*initial)
            }
        }
    }
}

impl Default for RetryWait {
    fn default() -> Self {
        Self::Constant {
            initial: Duration::from_millis(4000),
            iteration: 0,
        }
    }
}

