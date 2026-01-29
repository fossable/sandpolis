use crate::database::DatabaseLayer;
use crate::database::Resident;
use crate::database::ResidentVec;
use crate::realm::RealmName;
use crate::{ClusterId, InstanceId};
use anyhow::Result;
use chrono::{DateTime, Utc};
use futures_util::{SinkExt, StreamExt};
use native_db::ToKey;
use native_model::Model;
use sandpolis_macros::data;
use serde::{Deserialize, Serialize};
use serde_with::chrono::serde::{ts_seconds, ts_seconds_option};
use std::sync::RwLock;
use std::{cmp::min, net::SocketAddr, sync::Arc, time::Duration};
use stream::{StreamId, StreamMessage};
pub use stream::{StreamRegistry, StreamRequester, StreamResponder};
use tokio_util::sync::CancellationToken;
use tracing::debug;

/// Trait for layers to register their stream responders on new connections.
pub trait RegisterResponders: Send + Sync + 'static {
    fn register_responders(&self, registry: &StreamRegistry);
}

/// Wrapper for collecting `RegisterResponders` implementations via inventory.
pub struct ResponderRegistration(pub &'static dyn RegisterResponders);

// SAFETY: The inner reference is 'static and the trait requires Send + Sync
unsafe impl Send for ResponderRegistration {}
unsafe impl Sync for ResponderRegistration {}

inventory::collect!(ResponderRegistration);

/// Returns an iterator over all registered responder handlers.
pub fn collected_responders() -> impl Iterator<Item = &'static dyn RegisterResponders> {
    inventory::iter::<ResponderRegistration>().map(|r| r.0)
}

pub mod cli;
pub mod config;
pub mod messages;
pub mod ping;
#[cfg(feature = "server")]
pub mod server;
pub mod stream;

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
#[cfg(feature = "server")]
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
    pub data: Resident<ConnectionData>,
    pub realm: RealmName,
    pub cluster_id: ClusterId,
    pub cancel: CancellationToken,
    pub streams: Arc<StreamRegistry>,
}

impl Drop for InstanceConnection {
    fn drop(&mut self) {
        self.cancel.cancel();
    }
}

impl InstanceConnection {
    /// Register a stream handler and return a sender for outbound messages.
    pub fn register_stream<S: stream::StreamRequester>(
        &self,
        handler: S,
    ) -> (StreamId, tokio::sync::mpsc::Sender<StreamMessage>)
    where
        S::Out: Send + 'static,
    {
        self.streams.register(handler)
    }

    /// Remove a stream (Drop handles cleanup on the handler).
    pub fn close_stream(&self, stream_id: StreamId) {
        self.streams.close(stream_id);
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
