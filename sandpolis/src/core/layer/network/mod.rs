use crate::core::InstanceId;
use anyhow::Result;
use std::{collections::HashMap, sync::Arc};
use stream::StreamSource;

pub mod stream;

pub struct NetworkLayerData {}

pub struct NetworkLayer {
    db: sled::Tree,

    sources: HashMap<u64, Arc<dyn StreamSource>>,
}

struct PingRequest;

pub struct PingResponse {
    pub time: u64,
    pub id: InstanceId,
    pub from: Option<PingResponse>,
}

impl NetworkLayer {
    /// Send a message to the given instance and measure the time/path it took.
    pub async fn ping(&self, id: InstanceId) -> Result<PingResponse> {}

    /// Request the server to coordinate a direct connection to the given agent.
    pub async fn direct_connect(&self, agent: InstanceId, port: Option<u16>) {}
}

/// Request the server for a new direct connection.
// #[from = "client"]
// #[to = "server"]
pub struct DirectConnectionRequest {
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
