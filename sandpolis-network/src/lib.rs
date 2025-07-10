use anyhow::Context;
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
use std::fmt::Write;
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
    server_cooldown: DynamicRetry,
}

#[derive(Clone)]
pub struct NetworkLayer {
    data: Resident<NetworkLayerData>,

    /// Outbound connections
    outbound: Arc<RwLock<Vec<Arc<Connection>>>>,
}

impl NetworkLayer {
    pub async fn new(
        config: NetworkLayerConfig,
        database: DatabaseLayer,
        realms: RealmLayer,
    ) -> Result<Self> {
        debug!("Initializing network layer");

        let network = Self {
            outbound: Arc::new(RwLock::new(Vec::new())),
            data: database.realm(RealmName::default())?.resident(())?,
        };

        for server_url in config.servers.clone().unwrap_or_default() {
            realms
                .realm(server_url.realm.clone())
                .context("Realm does not exist")?;
        }

        Ok(network)
    }

    pub fn direct_or_server(&self, id: InstanceId) {}

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

    pub async fn run(&self) {
        // TODO run "wait for cmd"
        for server in self.servers.iter() {
            server.run().await;
        }
    }

    /// Request the server to coordinate a direct connection to the given agent.
    pub async fn connect_client_agent(&self, agent: InstanceId, port: Option<u16>) {
        todo!()
    }

    pub fn connect_client_server(
        address: ServerUrl,
        cooldown: DynamicRetry,
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
}

/// Convenience type to be used as return of request handler.
pub type RequestResult<T> = Result<axum::Json<T>, axum::Json<T>>;

#[data(temporal)]
pub struct ConnectionData {
    #[secondary_key]
    pub _instance_id: InstanceId,

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

    pub strategy: ConnectionStrategy,

    #[serde(with = "ts_seconds")]
    pub established: DateTime<Utc>,

    #[serde(with = "ts_seconds_option")]
    pub disconnected: Option<DateTime<Utc>>,
}

pub struct Connection {
    client: RwLock<reqwest::Client>,
    pub data: Resident<ConnectionData>,
}

impl Connection {
    async fn request(&self, body: impl Serialize) -> Result<()> {
        // Serialize request and record bytes
        let body = serde_json::to_vec(&body)?;

        match self.data.read().strategy {
            ConnectionStrategy::Continuous => todo!(),
            ConnectionStrategy::Polling { schedule, timeout } => todo!(),
        }
        Ok(())
    }
}

pub struct InboundConnection {}

/// How long to wait to retry after an unsuccessful connection attempt.
#[derive(Serialize, Deserialize, Clone, PartialEq, Debug)]
pub struct DynamicRetry {
    /// Initial cooldown value
    pub initial: Duration,

    /// Number of retries required for the total cooldown to
    /// increase by a factor of the initial cooldown.
    pub constant: Option<f64>,

    /// Maximum cooldown value
    pub limit: Option<Duration>,

    pub iteration: u32,
}

impl Iterator for DynamicRetry {
    type Item = Duration;

    fn next(&mut self) -> Option<Self::Item> {
        let value = match self.constant {
            Some(constant) => Duration::from_millis(
                ((self.initial.as_millis() as f64)
                    * (self.initial.as_millis() as f64).powf(self.iteration as f64 / constant))
                    as u64,
            ),
            None => self.initial,
        };

        self.iteration += 1;

        Some(match self.limit {
            // Apply maximum limit
            Some(limit) => min(value, limit),
            None => value,
        })
    }
}

impl Default for DynamicRetry {
    fn default() -> Self {
        Self {
            initial: Duration::from_millis(4000),
            constant: None,
            limit: None,
            iteration: 0,
        }
    }
}

#[derive(Serialize, Deserialize, Clone, PartialEq, Debug, Default)]
pub enum ConnectionStrategy {
    /// The connection will never intentionally disconnect. This is best when
    /// latency is important.
    #[default]
    Continuous,

    /// The connection will intentionally disconnect. This is best when latency
    /// is not important.
    Polling {
        ///  
        schedule: Duration,

        /// How long the connection will stay alive
        timeout: Duration,
    },
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
    pub address: ServerUrl,
    realm: RealmName,
    cooldown: DynamicRetry,
    pub client: reqwest::Client,
    pub data: Resident<ConnectionData>,
}

impl ServerConnection {
    /// Run the connection routine forever.
    pub async fn run(&self) {
        loop {
            if self.data.read().iterations > 0 {
                sleep(self.cooldown.next(self.data.read().iterations)).await;
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

/// Locates a server instance over the network. These have a format like:
///
/// ```
/// https://example.com:8768/default
/// ```
///
/// With default information omitted, the URL can be as simple as:
///
/// ```
/// https://example.com
/// ```
#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct ServerUrl {
    host: String,
    port: u16,
    realm: RealmName,
}

impl ServerUrl {
    /// Resolve the URL into IP addresses.
    pub fn resolve(&self) -> Result<Vec<SocketAddr>> {
        Ok(format!("{}:{}", &self.host, &self.port)
            .to_socket_addrs()?
            .collect())
    }

    /// Official server port: <https://www.iana.org/assignments/service-names-port-numbers/service-names-port-numbers.xhtml?search=8768>
    pub const fn default_port() -> u16 {
        8768
    }

    /// Whether the URL points to localhost.
    pub fn is_localhost(&self) -> bool {
        if self.host == "localhost" {
            return true;
        }

        if let Ok(addr) = self.host.parse::<SocketAddr>() {
            return addr.ip().is_loopback();
        }

        return false;
    }
}

impl FromStr for ServerUrl {
    type Err = anyhow::Error;

    fn from_str(s: &str) -> Result<Self> {
        let s = match s.strip_prefix("https://") {
            Some(s) => s,
            None => s,
        };

        let (s, realm) = match s.split_once("/") {
            Some((s, realm)) => (s, realm.parse()?),
            None => (s, RealmName::default()),
        };

        let (host, port) = match s.split_once(":") {
            Some((s, port)) => (s, port.parse()?),
            None => (s, ServerUrl::default_port()),
        };

        Ok(match host.parse::<SocketAddr>() {
            Ok(_) => Self {
                host: host.to_string(),
                port,
                realm,
            },
            Err(_) => Self {
                // TODO regex
                host: host.to_string(),
                port,
                realm,
            },
        })
    }
}

impl Display for ServerUrl {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str("https://")?;
        f.write_str(&self.host)?;

        if self.port != ServerUrl::default_port() {
            f.write_str(":")?;
            f.write_str(&format!("{}", &self.port))?;
        }

        if self.realm != RealmName::default() {
            f.write_str("/")?;
            f.write_str(&self.realm.to_string())?;
        }
        Ok(())
    }
}

/// Unidirectional share of `Data` between two instances. Use creation date to
/// "catch up" on missed data.
pub struct ShareData {
    // transfer_ownership: bool
}
