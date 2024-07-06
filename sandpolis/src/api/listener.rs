use serde::Deserialize;
use serde::Serialize;

#[cfg(any(feature = "server", feature = "client"))]
#[derive(Serialize, Deserialize)]
pub struct PostListenerRequest {
    /// The listening address
    pub address: String,

    /// The listening port
    pub port: u16,
}

#[cfg(any(feature = "server", feature = "client"))]
#[derive(Serialize, Deserialize)]
pub enum PostListenerResponse {
    Ok,
    AccessDenied,
    InvalidPort,
}

#[cfg(any(feature = "server", feature = "client"))]
#[derive(Serialize, Deserialize)]
pub enum DeleteListenerResponse {
    Ok,
    AccessDenied,
}

#[cfg(any(feature = "server", feature = "client"))]
#[derive(Serialize, Deserialize)]
pub struct PutListenerRequest {}

#[cfg(any(feature = "server", feature = "client"))]
#[derive(Serialize, Deserialize)]
pub enum PutListenerResponse {
    Ok,
    AccessDenied,
}
