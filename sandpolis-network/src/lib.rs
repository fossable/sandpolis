use anyhow::Result;
use chrono::{serde::ts_seconds, DateTime, NaiveDate, Utc};
use futures::StreamExt;
use reqwest::{Certificate, ClientBuilder, Identity};
use reqwest_websocket::RequestBuilderExt;
use sandpolis_instance::InstanceId;
use serde::{Deserialize, Serialize};
use serde_with::chrono::serde::ts_seconds_option;
use std::{cmp::min, collections::HashMap, net::SocketAddr, sync::Arc, time::Duration};
use tokio::time::sleep;
use tracing::debug;

use super::server::{
    group::{GroupClientCert, GroupName},
    ServerAddress,
};

pub mod stream;

pub struct NetworkLayerData {}

pub struct NetworkLayer {
    db: sled::Tree,

    connection: ServerConnection,
}

struct PingRequest;

pub struct PingResponse {
    pub time: u64,
    pub id: InstanceId,
    pub from: Option<Box<PingResponse>>,
}

/// Convenience type to be used as return of request handler.
#[cfg(any(feature = "server", feature = "agent"))]
pub type RequestResult<T> = Result<axum::Json<T>, axum::Json<T>>;

impl NetworkLayer {
    /// Send a message to the given instance and measure the time/path it took.
    pub async fn ping(&self, id: InstanceId) -> Result<PingResponse> {
        todo!()
    }

    /// Request the server to coordinate a direct connection to the given agent.
    pub async fn direct_connect(&self, agent: InstanceId, port: Option<u16>) {}
}

#[derive(Serialize, Deserialize)]
pub struct ConnectionData {
    /// Total number of bytes read since the connection was established
    read_bytes: u64,

    /// Total number of bytes written since the connection was established
    write_bytes: u64,

    /// "Recent" read throughput in bytes/second
    read_throughput: u64,

    /// "Recent" write throughput in bytes/second
    write_throughput: u64,

    /// Maximum read throughput in bytes/second since the connection was established
    read_throughput_max: u64,

    /// Maximum write throughput in bytes/second since the connection was established
    write_throughput_max: u64,

    local_socket: SocketAddr,
    remote_socket: SocketAddr,

    #[serde(with = "ts_seconds")]
    established: DateTime<Utc>,

    #[serde(with = "ts_seconds_option")]
    disconnected: Option<DateTime<Utc>>,
}

/// Request the server for a new direct connection to an agent.
// #[from = "client"]
// #[to = "server"]
pub struct AgentConnectionRequest {
    // The requested node
    id: InstanceId,

    // An optional listener port. If specified, the requested node will attempt
    // a connection on this port. Otherwise, the server will coordinate the connection.
    port: Option<u16>,
}

// Request that the recieving instance establish a new connection to the given host.
// message RQ_CoordinateConnection {

//     // The host IP address
//     string host = 1;

//     // The port
//     int32 port = 2;

//     // The transport protocol type
//     string transport = 3;

//     // The initial encryption key for the new connection.
//     bytes encryption_key = 4;
// }

/// A direct connection to an agent from a client.
pub struct AgentConnection {}

pub struct ConnectionCooldown {
    /// Initial cooldown value
    pub initial: Duration,

    /// Number of connection iterations required for the total cooldown to increase
    /// by a factor of the initial cooldown.
    pub constant: Option<f64>,

    /// Maximum cooldown value
    pub limit: Option<Duration>,
}

impl ConnectionCooldown {
    fn next(&self, iteration: u64) -> Duration {
        let value = match self.constant {
            Some(constant) => Duration::from_millis(
                ((self.initial.as_millis() as f64)
                    * (self.initial.as_millis() as f64).powf(iteration as f64 / constant))
                    as u64,
            ),
            None => self.initial,
        };

        // Apply maximum limit
        match self.limit {
            Some(limit) => min(value, limit),
            None => value,
        }
    }
}

/// A connection to a server from any other instance (including another server).
///
/// In continuous mode, the agent maintains its primary connection at all times. If
/// the connection is lost, the agent will periodically attempt to reestablish the
/// connection using the same parameters it used to establish the initial
/// connection.
///
/// The connection mode can be changed on-the-fly by a user or scheduled to change
/// automatically according to the time and day.
///
/// In polling mode, the agent intentionally closes the primary connection unless
/// there exists an active stream. On a configurable schedule, the agent reconnects
/// to a server, flushes any cached data, and checks for any new work items. After
/// executing all available work items, the primary connection is closed again.
///
/// The agent may attempt a spontaneous connection outside of the regular schedule
/// if an internal agent process triggers it.
pub struct ServerConnection {
    group: GroupName,
    iterations: u64, // TODO Data?
    cooldown: ConnectionCooldown,
    client: reqwest::Client,
}

impl ServerConnection {
    pub fn new(
        auth: GroupClientCert,
        addresses: Vec<ServerAddress>,
        cooldown: ConnectionCooldown,
    ) -> Result<Self> {
        let ca = auth.ca()?;
        let identity = auth.identity()?;

        Ok(Self {
            group: auth.name()?,
            iterations: 0,
            cooldown,
            client: ClientBuilder::new()
                .add_root_certificate(ca.clone())
                .identity(identity.clone())
                .resolve_to_addrs(
                    &auth.name()?,
                    &addresses
                        .iter()
                        .filter_map(|address| address.resolve().ok())
                        .flat_map(|address| address)
                        .collect::<Vec<SocketAddr>>(),
                )
                .build()
                .unwrap(),
        })
    }

    /// Run the connection routine forever.
    pub async fn run(mut self) {
        loop {
            if self.iterations > 0 {
                sleep(self.cooldown.next(self.iterations)).await;
            }

            debug!("Attempting server connection");
            match self
                .client
                .get(format!("wss://{}/", self.group))
                .header("Host", &*self.group)
                .upgrade()
                .send()
                .await
            {
                Ok(response) => match response.into_websocket().await {
                    // TODO include instance ID in the response
                    Ok(websocket) => {
                        let (sender, receiver) = websocket.split();
                        // TODO
                    }
                    Err(e) => debug!(error = ?e, "Connection upgrade failed"),
                },
                Err(e) => debug!(error = ?e, "Connection failed"),
            }
            self.iterations += 1;
        }
    }
}
