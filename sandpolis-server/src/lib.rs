use crate::banner::{GetBannerRequest, GetBannerResponse};
use crate::login::{LoginRequest, LoginResponse};
use anyhow::{Result, anyhow};
use axum::http::HeaderValue;
use cron::Schedule;
use native_db::ToKey;
use native_model::Model;
use reqwest::header::CONTENT_TYPE;
use reqwest::{ClientBuilder, Method};
use sandpolis_instance::database::DatabaseLayer;
use sandpolis_instance::database::Resident;
use sandpolis_instance::database::ResidentVec;
use sandpolis_instance::network::{
    ConnectionData, InstanceConnection, NetworkLayer, RetryWait, collected_responders,
};
use sandpolis_instance::realm::RealmLayer;
use sandpolis_instance::realm::RealmName;
use sandpolis_instance::{ClusterId, InstanceId};
use sandpolis_macros::data;
use serde::{Deserialize, Serialize, de::DeserializeOwned};
use std::fmt::Display;
use std::net::{IpAddr, SocketAddr, ToSocketAddrs};
use std::str::FromStr;
use std::sync::{Arc, RwLock};
use std::time::Duration;
use tokio_util::sync::CancellationToken;
use tracing::{debug, info};
use url::Url;
use validator::Validate;

pub mod banner;
#[cfg(feature = "server")]
pub mod block;
#[cfg(not(target_os = "android"))]
pub mod cli;
#[cfg(feature = "client")]
pub mod client;
pub mod config;
pub mod location;
pub mod login;
pub mod user;

#[data]
#[derive(Default)]
pub struct ServerLayerData {}

#[derive(Clone)]
#[cfg_attr(feature = "client", derive(bevy::prelude::Resource))]
pub struct ServerLayer {
    #[cfg(feature = "server")]
    pub banner: Resident<banner::ServerBannerData>,
    pub network: NetworkLayer,
    pub realms: RealmLayer,
    pub database: DatabaseLayer,
    #[cfg(feature = "client")]
    pub servers: ResidentVec<client::SavedServerData>,

    /// Outbound connections to servers
    pub outbound: Arc<RwLock<Vec<Arc<ServerConnection>>>>,
}

impl ServerLayer {
    pub async fn new(
        database: DatabaseLayer,
        network: NetworkLayer,
        realms: RealmLayer,
    ) -> Result<Self> {
        // Purge stale ConnectionData rows left over from previous runs
        {
            let realm = database.realm(RealmName::default())?;
            let r = realm.r_transaction()?;
            let stale: Vec<ConnectionData> =
                r.scan().primary()?.all()?.collect::<Result<Vec<_>, _>>()?;
            drop(r);
            if !stale.is_empty() {
                let rw = realm.rw_transaction()?;
                for row in stale {
                    rw.remove(row)?;
                }
                rw.commit()?;
            }
        }

        Ok(Self {
            #[cfg(feature = "server")]
            banner: database.realm(RealmName::default())?.resident(())?,
            network,
            realms,
            database: database.clone(),
            #[cfg(feature = "client")]
            servers: database.realm(RealmName::default())?.resident_vec(())?,
            outbound: Arc::new(RwLock::new(Vec::new())),
        })
    }

    /// Get all server connections.
    pub fn server_connections(&self) -> Vec<Arc<ServerConnection>> {
        let mut connections = self.outbound.read().unwrap().clone();
        connections.retain(|connection| connection.data.read().remote_instance.is_server());
        connections
    }

    #[cfg(any(feature = "agent", feature = "client"))] // Temporary
    fn connect_server(
        &self,
        url: ServerUrl,
        strategy: ServerConnectStrategy,
    ) -> Result<ServerConnection> {
        debug!(url = %url, ?strategy, "Configuring server connection");

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

        Ok(ServerConnection {
            inner: Arc::new(RwLock::new(None)),
            strategy,
            client: Arc::new(tokio::sync::RwLock::new(Some(client_builder()?))),
            data: self.database.realm(url.realm)?.resident(())?,
            cancel: CancellationToken::new(),
            banner: ServerBanner::default(),
            realm: cert.name()?,
            cluster_id: cert.cluster_id()?,
        })
    }

    /// Connect to a server in the default `Continuous` strategy (the live
    /// connection is held open by the websocket).
    #[cfg(any(feature = "agent", feature = "client"))] // Temporary
    pub async fn connect(&self, url: ServerUrl) -> Result<ServerConnection> {
        self.connect_with_strategy(url, ServerConnectStrategy::Continuous)
            .await
    }

    /// Connect to a server with an explicit [`ServerConnectStrategy`]. Agents
    /// that only check in periodically pass `Polling`; everything else uses
    /// `Continuous` via [`connect`](Self::connect).
    #[cfg(any(feature = "agent", feature = "client"))] // Temporary
    pub async fn connect_with_strategy(
        &self,
        url: ServerUrl,
        strategy: ServerConnectStrategy,
    ) -> Result<ServerConnection> {
        let mut inner = self.connect_server(url, strategy)?;

        debug!("Fetching server banner");

        // Fetch banner before we return a complete connection
        let response: GetBannerResponse = inner
            .get(
                "server/banner",
                GetBannerRequest {
                    #[cfg(feature = "client")]
                    include_image: true,
                    #[cfg(not(feature = "client"))]
                    include_image: false,
                },
            )
            .await?;

        debug!(banner = ?response.0, "Fetched server banner");

        inner.banner = response.0;
        Ok(inner)
    }
}

/// Contains information about the server useful for prospective logins
#[derive(Serialize, Deserialize, Debug, Clone, PartialEq, Default)]
pub struct ServerBanner {
    /// Indicates that only admin users will be allowed to login
    pub maintenance: bool,

    /// A string to display on the login screen
    pub message: Option<String>,

    /// An image to display on the login screen
    #[serde(with = "serde_bytes")]
    pub image: Option<Vec<u8>>,

    /// Whether users are required to provide a second authentication mechanism
    /// on login
    pub mfa: bool,
}

impl Validate for ServerBanner {
    fn validate(&self) -> Result<(), validator::ValidationErrors> {
        if let Some(image_data) = &self.image {
            // Validate PNG format using png crate
            let cursor = std::io::Cursor::new(image_data);
            let decoder = png::Decoder::new(cursor);

            if decoder.read_info().is_err() {
                return Err(validator::ValidationErrors::new());
            }
        }

        Ok(())
    }
}

#[derive(Clone)]
pub struct ServerConnection {
    client: Arc<tokio::sync::RwLock<Option<reqwest::Client>>>,
    pub strategy: ServerConnectStrategy,
    pub data: Resident<ConnectionData>,
    pub cancel: CancellationToken,
    pub banner: ServerBanner,
    /// Active websocket connection used for streams / sync, once established.
    pub inner: Arc<RwLock<Option<Arc<InstanceConnection>>>>,
    pub realm: RealmName,
    pub cluster_id: ClusterId,
}

impl Drop for ServerConnection {
    fn drop(&mut self) {
        self.cancel.cancel();
    }
}

impl ServerConnection {
    /// Get the remote address of this connection.
    pub fn address(&self) -> Option<SocketAddr> {
        self.data.read().remote_socket
    }

    /// Establish the websocket connection used for streams and DB sync, retaining
    /// it on this `ServerConnection`. The connection is deliberately *not* added
    /// to `network.inbound`: that list backs the server-side stream relay, and in
    /// an all-in-one build a dialer-side connection there (whose peer is the
    /// local server itself) would be picked up as a relay target, bouncing
    /// messages back to the server instead of reaching the agent.
    #[cfg(any(feature = "client", feature = "agent"))]
    pub async fn open_websocket(
        &self,
        network: &NetworkLayer,
        instance_id: InstanceId,
    ) -> Result<Arc<InstanceConnection>> {
        use reqwest_websocket::Upgrade;

        let url = format!("https://{}.{}/connect", self.cluster_id, self.realm);
        let response = {
            let guard = self.client.read().await;
            let client = guard
                .as_ref()
                .ok_or_else(|| anyhow!("connection has no http client"))?;
            client
                .get(&url)
                .header("x-realm", self.realm.to_string())
                .header("x-instance-id", instance_id.to_string())
                .upgrade()
                .send()
                .await?
        };
        // The server reports its own instance id in the upgrade response so we can
        // record the real peer instead of a freshly-generated default (which would
        // surface as a phantom graph node, growing on every reconnect).
        let remote_instance = response
            .headers()
            .get("x-instance-id")
            .and_then(|v| v.to_str().ok())
            .and_then(|s| s.parse::<InstanceId>().ok());

        let socket = response.into_websocket().await?;

        let mut cd = ConnectionData::default();
        if let Some(id) = remote_instance {
            cd.remote_instance = id;
        }
        cd.established = chrono::Utc::now();
        let data = network.connections.push(cd).map_err(|e| anyhow!("{e}"))?;

        // Serve our local realm database to the peer's sync subscriptions
        // (an agent answering the server's all-filter requester).
        let realm_db = network.database.realm(self.realm.clone())?;
        let sync_reg = sandpolis_instance::network::sync::SyncResponderRegistration::new(realm_db);
        let mut handlers: Vec<&dyn sandpolis_instance::network::RegisterResponders> =
            collected_responders().collect();
        handlers.push(&sync_reg);
        let connection = InstanceConnection::websocket_client(
            socket,
            data,
            self.realm.clone(),
            self.cluster_id,
            &handlers,
        );
        *self.inner.write().unwrap() = Some(connection.clone());
        Ok(connection)
    }

    /// Close the sync websocket opened by [`open_websocket`](Self::open_websocket),
    /// if one is active. Agents in `Polling` mode call this to end a check-in
    /// window: the socket is cancelled and its bookkeeping (the tracked
    /// `ConnectionData` row) is cleaned up so repeated windows don't accumulate
    /// stale connections.
    #[cfg(any(feature = "client", feature = "agent"))]
    pub fn close_websocket(&self, network: &NetworkLayer) {
        let Some(connection) = self.inner.write().unwrap().take() else {
            return;
        };

        // Cancel the socket task explicitly. `Drop` would also do this once every
        // Arc is gone, but a stream may still hold a reference.
        connection.cancel.cancel();

        let id = connection.data.read()._id;
        let _ = network.connections.remove(id);
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

        // One-off requests use the pooled `reqwest` client regardless of
        // strategy: it opens connections on demand, so this works the same
        // whether the agent holds a `Continuous` websocket or only connects
        // during a `Polling` window. The strategy only governs the lifetime of
        // the long-lived sync websocket (see `open_websocket`).
        debug!(endpoint = %endpoint, "Sending request");
        let guard = self.client.read().await;
        let client = guard
            .as_ref()
            .ok_or_else(|| anyhow!("connection has no http client"))?;

        Ok(client
            .request(
                method,
                format!("https://{}.{}/{endpoint}", self.cluster_id, self.realm),
            )
            .header(CONTENT_TYPE, HeaderValue::from_static("application/json"))
            .header("x-realm", self.realm.to_string())
            .body(body)
            .send()
            .await?
            .json()
            .await?)
    }
}

impl ServerConnection {
    pub async fn login(&self, request: LoginRequest) -> Result<LoginResponse> {
        // TODO span username
        debug!(username = %request.username, "Attempting login");

        let result = self.post("user/login", request).await;
        if let Ok(LoginResponse::Ok(_)) = result {
            info!("Login succeeded");
        }
        result
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
pub enum ServerConnectStrategy {
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

impl ServerConnectStrategy {
    /// Build a [`Polling`](ServerConnectStrategy::Polling) strategy from a cron
    /// expression and a per-window keep-alive duration. Lets callers construct
    /// the strategy without depending on the `cron` crate directly.
    pub fn polling(schedule: &str, timeout: Duration) -> Result<Self> {
        Ok(Self::Polling {
            schedule: Schedule::from_str(schedule)
                .map_err(|e| anyhow!("invalid cron schedule {schedule:?}: {e}"))?,
            timeout,
        })
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
/// ```text
/// https://example.com:8768/default
/// ```
///
/// With default information omitted, the URL can be as simple as:
///
/// ```text
/// https://example.com
/// ```
#[derive(Serialize, Deserialize, PartialEq, Clone, Debug)]
pub struct ServerUrl {
    pub host: String,
    pub port: u16,
    pub realm: RealmName,
    pub retry: RetryWait,
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

        let host = url
            .host_str()
            .ok_or_else(|| anyhow!("Invalid host in URL"))?;
        let host = host
            .strip_prefix('[')
            .and_then(|h| h.strip_suffix(']'))
            .unwrap_or(host)
            .to_string();

        Ok(Self {
            host,
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
        let url: ServerUrl = "example.com/myrealm".parse().unwrap();
        assert_eq!(url.host, "example.com");
        assert_eq!(url.port, 8768);
        assert_eq!(url.realm, "myrealm".parse().unwrap());
    }

    #[test]
    fn test_server_url_from_str_full() {
        let url: ServerUrl = "https://example.com:9000/myrealm".parse().unwrap();
        assert_eq!(url.host, "example.com");
        assert_eq!(url.port, 9000);
        assert_eq!(url.realm, "myrealm".parse().unwrap());
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
            realm: "myrealm".parse().unwrap(),
            retry: RetryWait::default(),
        };
        assert_eq!(url.to_string(), "https://example.com/myrealm");
    }

    #[test]
    fn test_server_url_display_full() {
        let url = ServerUrl {
            host: "example.com".to_string(),
            port: 9000,
            realm: "myrealm".parse().unwrap(),
            retry: RetryWait::default(),
        };
        assert_eq!(url.to_string(), "https://example.com:9000/myrealm");
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
        let original = "https://example.com:9000/myrealm";
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

/// A group is a collection of instances within the same realm.
#[data]
pub struct GroupData {
    #[secondary_key(unique)]
    pub name: String,

    pub members: Vec<InstanceId>,
}
