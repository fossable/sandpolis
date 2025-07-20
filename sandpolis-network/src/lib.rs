#![feature(iterator_try_collect)]

use crate::messages::{PollRequest, PollResponse};
use anyhow::Context;
use anyhow::Result;
use anyhow::anyhow;
use anyhow::bail;
use chrono::{DateTime, Utc};
use config::NetworkLayerConfig;
use cron::Schedule;
use futures_util::StreamExt;
use messages::PingRequest;
use messages::PingResponse;
use native_db::ToKey;
use native_model::Model;
use reqwest::ClientBuilder;
use reqwest::Method;
use reqwest_websocket::RequestBuilderExt;
use sandpolis_core::ClusterId;
use sandpolis_core::{InstanceId, RealmName};
use sandpolis_database::DatabaseLayer;
use sandpolis_database::Resident;
use sandpolis_macros::data;
use sandpolis_realm::RealmLayer;
use serde::de::DeserializeOwned;
use serde::{Deserialize, Serialize};
use serde_with::chrono::serde::{ts_seconds, ts_seconds_option};
use std::fmt::Display;
use std::net::IpAddr;
use std::net::ToSocketAddrs;
use std::str::FromStr;
use std::sync::RwLock;
use std::{cmp::min, net::SocketAddr, sync::Arc, time::Duration};
use tokio::time::sleep;
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

#[cfg(feature = "bootagent")]
pub mod bootagent;

#[data]
#[derive(Default)]
pub struct NetworkLayerData {}

#[derive(Clone)]
pub struct NetworkLayer {
    data: Resident<NetworkLayerData>,

    /// Outbound connections
    pub outbound: Arc<RwLock<Vec<Arc<OutboundConnection>>>>,

    realms: RealmLayer,

    database: DatabaseLayer,
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
            database,
            realms: realms.clone(),
        };

        // Initiate connections to configured servers before we return
        #[cfg(any(feature = "agent", feature = "client"))] // Temporary
        for server_url in config.servers.clone().unwrap_or_default() {
            network.connect_server(server_url)?;
        }

        Ok(network)
    }

    /// Find any outbound server connection.
    pub fn find_server(&self) -> Option<Arc<OutboundConnection>> {
        for connection in self.outbound.read().unwrap().iter() {
            if connection.data.read().remote_instance.is_server() {
                return Some(connection.clone());
            }
        }
        None
    }

    /// Find a direct outbound connection to the given instance.
    pub fn find_instance(&self, id: InstanceId) -> Option<Arc<OutboundConnection>> {
        for connection in self.outbound.read().unwrap().iter() {
            if connection.data.read().remote_instance == id {
                return Some(connection.clone());
            }
        }
        None
    }

    /// Send a message to the given instance and measure the time/path it took.
    pub async fn ping(&self, id: InstanceId) -> Result<PingResponse> {
        let connection = self
            .find_instance(id)
            .or_else(|| self.find_server())
            .ok_or(anyhow!("Failed to find suitable connection"))?;

        let response: PingResponse = connection.get("/ping", PingRequest { id }).await?;
        todo!()
    }

    /// Request the server to coordinate a direct connection to the given agent.
    pub fn connect_agent(&self, agent: InstanceId, port: Option<u16>) {
        todo!()
    }

    #[cfg(any(feature = "agent", feature = "client"))] // Temporary
    pub fn connect_server(&self, url: ServerUrl) -> Result<OutboundConnection> {
        debug!(url = %url, "Configuring server connection");

        // Locate the realm certificate
        #[cfg(feature = "client")]
        let cert = self.realms.find_client_cert(url.realm.clone())?;

        #[cfg(feature = "agent")]
        let cert = self.realms.find_agent_cert(url.realm.clone())?;

        let client_builder = || -> Result<reqwest::Client> {
            Ok(ClientBuilder::new()
                .add_root_certificate(cert.ca()?)
                .identity(cert.identity()?)
                .resolve_to_addrs(
                    &format!("{}.{}", cert.cluster_id()?, &cert.name()?),
                    &url.resolve()?,
                )
                .build()
                .unwrap())
        };

        let token = CancellationToken::new();

        let connection = OutboundConnection {
            strategy: ConnectionStrategy::Continuous,
            client: Arc::new(tokio::sync::RwLock::new(Some(client_builder()?))),
            data: self.database.realm(url.realm)?.resident(())?,
            retry: Arc::new(RwLock::new(url.retry)),
            cancel: token.clone(),
            realm: cert.name()?,
            cluster_id: cert.cluster_id()?,
        };
        let connection_clone = connection.clone();

        tokio::spawn({
            async move {
                loop {
                    tokio::select! {
                        _ = token.cancelled() => {
                            break;
                        }
                        response = connection.get::<PollResponse>("poll", PollRequest) => {
                            match response {
                                Ok(poll_response) => {
                                    // Pass command to admin socket

                                },
                                Err(e) => {
                                    debug!(error = %e, "Poll request failed");
                                    // Wait before retrying
                                    let timeout = {connection.retry.write().unwrap().next().unwrap()};
                                    sleep(timeout).await;
                                },
                            }
                        }
                    }
                }
            }
        });

        Ok(connection_clone)
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

#[derive(Clone)]
pub struct OutboundConnection {
    client: Arc<tokio::sync::RwLock<Option<reqwest::Client>>>,
    pub strategy: ConnectionStrategy,
    pub data: Resident<ConnectionData>,
    pub retry: Arc<RwLock<RetryWait>>,
    pub cancel: CancellationToken,
    pub realm: RealmName,
    pub cluster_id: ClusterId,
}

impl Drop for OutboundConnection {
    fn drop(&mut self) {
        self.cancel.cancel();
    }
}

impl OutboundConnection {
    /// Get the remote address of this connection.
    pub fn address(&self) -> Option<SocketAddr> {
        self.data.read().remote_socket
    }

    pub async fn get<Response>(&self, endpoint: &str, body: impl Serialize) -> Result<Response>
    where
        Response: DeserializeOwned,
    {
        self.request(Method::GET, endpoint, body).await
    }

    pub async fn post<Response>(&self, endpoint: &str, body: impl Serialize) -> Result<Response>
    where
        Response: DeserializeOwned,
    {
        self.request(Method::POST, endpoint, body).await
    }

    pub async fn request<Response>(
        &self,
        method: Method,
        endpoint: &str,
        body: impl Serialize,
    ) -> Result<Response>
    where
        Response: DeserializeOwned,
    {
        // Serialize request and record bytes
        let body = serde_json::to_vec(&body)?;

        match &self.strategy {
            ConnectionStrategy::Continuous => {
                debug!(endpoint = %endpoint, "Sending request");
                let guard = self.client.read().await;
                let client = guard.as_ref().unwrap();

                Ok(client
                    .request(
                        method,
                        format!("https://{}.{}/{endpoint}", self.cluster_id, self.realm),
                    )
                    .body(body)
                    .send()
                    .await?
                    .json()
                    .await?)
            }
            ConnectionStrategy::Polling { schedule, timeout } => {
                // Get the next scheduled time from the cron schedule
                let next_time = schedule.upcoming(chrono::Utc).next();
                todo!(
                    "Implement cron-based polling with next_time: {:?}, timeout: {:?}",
                    next_time,
                    timeout
                )
            }
        }
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
#[derive(Serialize, Deserialize, Clone, PartialEq, Debug, Default)]
pub enum ConnectionStrategy {
    /// The connection will never intentionally disconnect. This is best when
    /// latency is important.
    #[default]
    Continuous,

    /// The connection will intentionally disconnect. This is best when latency
    /// is not important.
    Polling {
        /// Cron schedule for when to poll (e.g., "0 */5 * * * *" for every 5
        /// minutes)
        schedule: Schedule,

        /// How long the connection will stay alive
        timeout: Duration,
    },
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
#[derive(Serialize, Deserialize, PartialEq, Clone, Debug)]
pub struct ServerUrl {
    host: String,
    port: u16,
    realm: RealmName,
    retry: RetryWait,
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

        if let Ok(ip) = self.host.parse::<IpAddr>() {
            return ip.is_loopback();
        }

        false
    }
}

impl FromStr for ServerUrl {
    type Err = anyhow::Error;

    fn from_str(s: &str) -> Result<Self> {
        let url = Url::parse(&if s.starts_with("https://") {
            s.to_string()
        } else {
            format!("https://{s}")
        })?;

        // TODO
        url.query_pairs();

        Ok(Self {
            host: url
                .host_str()
                .ok_or_else(|| anyhow!("Invalid host in URL"))?
                .to_string(),
            port: url.port().unwrap_or(ServerUrl::default_port()),
            realm: if url.path().len() > 1 {
                url.path().trim_start_matches('/').parse()?
            } else {
                RealmName::default()
            },
            // TODO
            retry: RetryWait::default(),
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

        if self.retry != RetryWait::default() {
            match self.retry {
                RetryWait::Exponential {
                    initial,
                    constant,
                    limit,
                    iteration: _,
                } => {
                    f.write_str(&format!(
                        "?type=exponential&initial={}&constant={}",
                        initial.as_millis(),
                        constant,
                    ))?;
                    if let Some(l) = limit {
                        f.write_str(&format!("&limit={}", l.as_millis(),))?;
                    }
                }
                RetryWait::Constant {
                    initial,
                    iteration: _,
                } => f.write_str(&format!("?type=constant&initial={}", initial.as_millis()))?,
            }
        }
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_server_url_from_str_basic() {
        let url: ServerUrl = "example.com".parse().unwrap();
        assert_eq!(url.host, "example.com");
        assert_eq!(url.port, 8768);
        assert_eq!(url.realm, RealmName::default());
    }

    #[test]
    fn test_server_url_from_str_with_port() {
        let url: ServerUrl = "example.com:9000".parse().unwrap();
        assert_eq!(url.host, "example.com");
        assert_eq!(url.port, 9000);
        assert_eq!(url.realm, RealmName::default());
    }

    #[test]
    fn test_server_url_from_str_with_https() {
        let url: ServerUrl = "https://example.com".parse().unwrap();
        assert_eq!(url.host, "example.com");
        assert_eq!(url.port, 8768);
        assert_eq!(url.realm, RealmName::default());
    }

    #[test]
    fn test_server_url_from_str_with_realm() {
        let url: ServerUrl = "example.com/my-realm".parse().unwrap();
        assert_eq!(url.host, "example.com");
        assert_eq!(url.port, 8768);
        assert_eq!(url.realm, "my-realm".parse().unwrap());
    }

    #[test]
    fn test_server_url_from_str_full() {
        let url: ServerUrl = "https://example.com:9000/my-realm".parse().unwrap();
        assert_eq!(url.host, "example.com");
        assert_eq!(url.port, 9000);
        assert_eq!(url.realm, "my-realm".parse().unwrap());
    }

    #[test]
    fn test_server_url_from_str_ip_address() {
        let url: ServerUrl = "192.168.1.1:8080".parse().unwrap();
        assert_eq!(url.host, "192.168.1.1");
        assert_eq!(url.port, 8080);
        assert_eq!(url.realm, RealmName::default());
    }

    #[test]
    fn test_server_url_display_default() {
        let url = ServerUrl {
            host: "example.com".to_string(),
            port: 8768,
            realm: RealmName::default(),
            retry: RetryWait::default(),
        };
        assert_eq!(url.to_string(), "https://example.com");
    }

    #[test]
    fn test_server_url_display_with_port() {
        let url = ServerUrl {
            host: "example.com".to_string(),
            port: 9000,
            realm: RealmName::default(),
            retry: RetryWait::default(),
        };
        assert_eq!(url.to_string(), "https://example.com:9000");
    }

    #[test]
    fn test_server_url_display_with_realm() {
        let url = ServerUrl {
            host: "example.com".to_string(),
            port: 8768,
            realm: "my-realm".parse().unwrap(),
            retry: RetryWait::default(),
        };
        assert_eq!(url.to_string(), "https://example.com/my-realm");
    }

    #[test]
    fn test_server_url_display_full() {
        let url = ServerUrl {
            host: "example.com".to_string(),
            port: 9000,
            realm: "my-realm".parse().unwrap(),
            retry: RetryWait::default(),
        };
        assert_eq!(url.to_string(), "https://example.com:9000/my-realm");
    }

    #[test]
    fn test_server_url_is_localhost() {
        let url = ServerUrl {
            host: "localhost".to_string(),
            port: 8768,
            realm: RealmName::default(),
            retry: RetryWait::default(),
        };
        assert!(url.is_localhost());

        let url = ServerUrl {
            host: "127.0.0.1".to_string(),
            port: 8768,
            realm: RealmName::default(),
            retry: RetryWait::default(),
        };
        assert!(url.is_localhost());

        let url = ServerUrl {
            host: "example.com".to_string(),
            port: 8768,
            realm: RealmName::default(),
            retry: RetryWait::default(),
        };
        assert!(!url.is_localhost());
    }

    #[test]
    fn test_server_url_default_port() {
        assert_eq!(ServerUrl::default_port(), 8768);
    }

    #[test]
    fn test_server_url_roundtrip() {
        let original = "https://example.com:9000/my-realm";
        let url: ServerUrl = original.parse().unwrap();
        assert_eq!(url.to_string(), original);
    }

    #[test]
    fn test_server_url_invalid_scheme() {
        let result: Result<ServerUrl, _> = "http://example.com".parse();
        assert!(result.is_err());
    }

    #[test]
    fn test_server_url_invalid_host() {
        let result: Result<ServerUrl, _> = "https://".parse();
        assert!(result.is_err());
    }

    #[test]
    fn test_server_url_ipv6() {
        let url: ServerUrl = "[::1]:8080".parse().unwrap();
        assert_eq!(url.host, "::1");
        assert_eq!(url.port, 8080);
        assert!(url.is_localhost());
    }
}

/// Unidirectional share of `Data` between two instances. Use creation date to
/// "catch up" on missed data.
pub struct ShareData {
    // transfer_ownership: bool
}
