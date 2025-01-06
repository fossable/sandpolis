use crate::core::InstanceId;
use anyhow::Result;
use chrono::{serde::ts_seconds, DateTime, NaiveDate, Utc};
use futures::StreamExt;
use reqwest::ClientBuilder;
use reqwest_websocket::RequestBuilderExt;
use serde::{Deserialize, Serialize};
use serde_with::chrono::serde::ts_seconds_option;
use std::{cmp::min, collections::HashMap, net::SocketAddr, sync::Arc, time::Duration};
use tokio::time::sleep;
use tracing::debug;

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
    /// Initial cooldown value in milliseconds
    pub initial_ms: u64,
    /// Number of connection iterations required for the total cooldown to increase
    /// by a factor of the initial cooldown. Zero disables cooldown increase.
    pub constant: f64,

    /// Maximum cooldown value in milliseconds
    pub limit_ms: u64,
}

impl ConnectionCooldown {
    fn next(&self, iteration: u64) -> Duration {
        Duration::from_millis(min(
            self.limit_ms,
            ((self.initial_ms as f64)
                * (self.initial_ms as f64).powf(iteration as f64 / self.constant))
                as u64,
        ))
    }
}

/// A "continuous" or "polling" connection to a server from any other instance.
pub struct ServerConnection {
    iterations: u64, // TODO Data?
    cooldown: Option<ConnectionCooldown>,
    connections: HashMap<String, reqwest::Client>,
}

impl ServerConnection {
    pub fn new(addresses: Vec<String>, cooldown: Option<ConnectionCooldown>) -> Self {
        Self {
            iterations: 0,
            cooldown,
            connections: addresses
                .into_iter()
                .map(|a| (a, ClientBuilder::new().build().unwrap()))
                .collect(),
        }
    }

    /// Run the connection routine forever.
    pub async fn run(mut self) {
        loop {
            if self.iterations > 0 {
                if let Some(cooldown) = self.cooldown.as_ref() {
                    sleep(cooldown.next(self.iterations)).await;
                }
            }

            for (address, client) in self.connections.iter() {
                debug!(address = %address, "Attempting server connection");
                match client
                    .get(format!("wss://{address}/"))
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
                        Err(_) => debug!(address = %address, "Connection upgrade failed"),
                    },
                    Err(_) => debug!(address = %address, "Connection failed"),
                }
            }
        }
    }
}
