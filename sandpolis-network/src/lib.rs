use anyhow::Result;
use anyhow::anyhow;
use anyhow::bail;
use chrono::{DateTime, Utc};
use config::NetworkLayerConfig;
use futures_util::StreamExt;
use messages::PingRequest;
use messages::PingResponse;
use native_db::ToKey;
use native_model::Model;
use reqwest::ClientBuilder;
use reqwest_websocket::RequestBuilderExt;
use sandpolis_core::{InstanceId, RealmName};
use sandpolis_database::DatabaseLayer;
use sandpolis_database::Resident;
use sandpolis_macros::data;
use sandpolis_realm::RealmClientCert;
use sandpolis_realm::RealmLayer;
use serde::{Deserialize, Serialize};
use serde_with::chrono::serde::{ts_seconds, ts_seconds_option};
use std::fmt::Display;
use std::net::ToSocketAddrs;
use std::str::FromStr;
use std::{cmp::min, net::SocketAddr, sync::Arc, time::Duration};
use tokio::sync::RwLock;
use tokio::time::sleep;
use tracing::debug;

pub mod cli;
pub mod config;
pub mod messages;
pub mod routes;
pub mod stream;

#[cfg(feature = "server")]
pub mod server;

#[data]
pub struct NetworkLayerData {
    server_cooldown: ConnectionCooldown,
}

#[derive(Clone)]
pub struct NetworkLayer {
    data: Resident<NetworkLayerData>,
    pub servers: Arc<Vec<ServerConnection>>,
}

impl NetworkLayer {
    pub async fn new(
        config: NetworkLayerConfig,
        database: DatabaseLayer,
        realm: RealmLayer,
    ) -> Result<Self> {
        let cert = if config.servers.is_none() {
            if cfg!(feature = "agent") {
                bail!("No servers given")
            } else {
                None
            }
        } else {
            // Make sure we have a clientAuth cert
            Some(
                // realm
                //     .data
                //     .data
                //     .client
                //     .ok_or(anyhow!("No realm certificate found"))?,
                todo!(),
            )
        };

        Ok(Self {
            servers: Arc::new(
                config
                    .servers
                    .as_ref()
                    .unwrap_or(&Vec::new())
                    .into_iter()
                    .map(|address| {
                        ServerConnection::new(
                            address.to_owned(),
                            // data.data.server_cooldown.clone(),
                            todo!(),
                            cert.clone().ok_or(anyhow!(""))?,
                        )
                    })
                    .collect::<Result<Vec<ServerConnection>>>()?,
            ),
            data: database.realm(RealmName::default())?.resident(())?,
        })
    }

    /// Send a message to the given instance and measure the time/path it took.
    pub async fn ping(&self, id: InstanceId) -> Result<PingResponse> {
        let response: PingResponse = self.servers[0] // TODO select best
            .client
            .get("/ping")
            .json(&PingRequest { id })
            .send()
            .await?
            .json()
            .await?;
        todo!()
    }

    /// Request the server to coordinate a direct connection to the given agent.
    pub async fn direct_connect(&self, agent: InstanceId, port: Option<u16>) {
        todo!()
    }

    pub async fn run(&self) {
        for server in self.servers.iter() {
            server.run().await;
        }
    }
}

/// Convenience type to be used as return of request handler.
pub type RequestResult<T> = Result<axum::Json<T>, axum::Json<T>>;

#[data(temporal)]
pub struct ConnectionData {
    #[secondary_key]
    pub _instance_id: InstanceId,

    pub remote_instance: InstanceId,

    /// Total number of bytes read since the connection was established
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

/// A direct connection to an agent from a client.
pub struct AgentConnection {}

#[derive(Serialize, Deserialize, Clone, PartialEq, Debug)]
pub struct ConnectionCooldown {
    /// Initial cooldown value
    pub initial: Duration,

    /// Number of connection iterations required for the total cooldown to
    /// increase by a factor of the initial cooldown.
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

impl Default for ConnectionCooldown {
    fn default() -> Self {
        Self {
            initial: Duration::from_millis(4000),
            constant: None,
            limit: None,
        }
    }
}

pub struct ServerConnectionData {
    iterations: u64,
}

/// A connection to a server from any other instance (including another server).
///
/// In continuous mode, the agent maintains its primary connection at all times.
/// If the connection is lost, the agent will periodically attempt to
/// reestablish the connection using the same parameters it used to establish
/// the initial connection.
///
/// The connection mode can be changed on-the-fly by a user or scheduled to
/// change automatically according to the time and day.
///
/// In polling mode, the agent intentionally closes the primary connection
/// unless there exists an active stream. On a configurable schedule, the agent
/// reconnects to a server, flushes any cached data, and checks for any new work
/// items. After executing all available work items, the primary connection is
/// closed again.
///
/// The agent may attempt a spontaneous connection outside of the regular
/// schedule if an internal agent process triggers it.
pub struct ServerConnection {
    pub address: ServerAddress,
    realm: RealmName,
    cooldown: ConnectionCooldown,
    pub client: reqwest::Client,
    pub data: RwLock<ServerConnectionData>,
}

impl ServerConnection {
    pub fn new(
        address: ServerAddress,
        cooldown: ConnectionCooldown,
        cert: RealmClientCert,
    ) -> Result<Self> {
        Ok(Self {
            realm: cert.name()?,
            data: RwLock::new(ServerConnectionData { iterations: 0 }),
            cooldown,
            client: ClientBuilder::new()
                .add_root_certificate(cert.ca()?)
                .identity(cert.identity()?)
                .resolve_to_addrs(&cert.name()?, &address.resolve()?)
                .build()
                .unwrap(),
            address,
        })
    }

    /// Run the connection routine forever.
    pub async fn run(&self) {
        loop {
            if self.data.read().await.iterations > 0 {
                sleep(self.cooldown.next(self.data.read().await.iterations)).await;
            }

            debug!("Attempting server connection");
            match self
                .client
                .get(format!("wss://{}/", self.realm))
                .header("Host", &*self.realm)
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
            self.data.write().await.iterations += 1;
        }
    }
}

/// There can be multiple servers in a Sandpolis network which improves both
/// scalability and failure tolerance. There are two roles in which a server can
/// exist: a global level and a local level.
#[derive(Serialize, Deserialize, Clone, Debug)]
pub enum ServerStratum {
    /// This server maintains data only for the agents that its directly
    /// connected to. Local stratum (LS) servers connect to at most one GS
    /// server. LS servers may not connect directly to each other.
    ///
    /// LS servers are optional, but may be useful for on-premise installations
    /// where the server can continue operating even when the network goes down.
    ///
    /// In raft terminology, LS servers are "learners" and don't participate in
    /// leader voting.
    Local,

    // TODO only one?
    /// This server maintains a complete copy of all data in the cluster. Global
    /// stratum (GS) servers connect to every other GS server (fully-connected)
    /// for data replication and leader election.
    Global,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub enum ServerAddress {
    Dns(String),
    Ip(SocketAddr),
}

impl ServerAddress {
    pub fn resolve(&self) -> Result<Vec<SocketAddr>> {
        match self {
            Self::Dns(name) => Ok(name.to_socket_addrs()?.collect()),
            Self::Ip(socket_addr) => Ok(vec![socket_addr.clone()]),
        }
    }

    /// Official server port: <https://www.iana.org/assignments/service-names-port-numbers/service-names-port-numbers.xhtml?search=8768>
    pub const fn default_port() -> u16 {
        8768
    }

    /// Whether the server is running on localhost.
    pub fn is_localhost(&self) -> bool {
        match self {
            ServerAddress::Dns(dns) => dns == "localhost",
            ServerAddress::Ip(ip) => ip.to_string().starts_with("127.0.0.1:"),
        }
    }
}

impl FromStr for ServerAddress {
    type Err = anyhow::Error;

    fn from_str(s: &str) -> Result<Self> {
        match s.parse::<SocketAddr>() {
            Ok(addr) => Ok(Self::Ip(addr)),
            // TODO regex
            Err(_) => Ok(Self::Dns(s.to_string())),
        }
    }
}

impl Display for ServerAddress {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            ServerAddress::Dns(dns) => f.write_str(&dns),
            ServerAddress::Ip(ip) => f.write_str(&ip.to_string()),
        }
    }
}

/// Unidirectional share of `Data` between two instances. Use creation date to
/// "catch up" on missed data.
pub struct ShareData {
    // transfer_ownership: bool
}
