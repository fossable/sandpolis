use sandpolis_core::InstanceId;
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize)]
pub struct PingRequest {
    pub id: InstanceId,
}

#[derive(Serialize, Deserialize)]
pub struct PingResponse {
    pub time: u64,
    pub id: InstanceId,
    pub from: Option<Box<PingResponse>>,
}

/// Request the server for a new direct connection to an agent.
#[cfg(any(feature = "server", feature = "client"))]
#[derive(Serialize, Deserialize)]
pub struct AgentConnectionRequest {
    /// Requested agent
    id: InstanceId,
    // TODO transport TCP/UDP?
    /// Current time of this instance which allows the server to forecast the
    /// connection start time.
    clock: u64,
}

#[cfg(any(feature = "server", feature = "client"))]
#[derive(Serialize, Deserialize)]
pub struct AgentConnectionResponse {
    /// Agent's public address
    address: String,

    /// Randomized port that the client will attempt a connection on.
    port: u16,

    /// Randomized encryption key for the connection
    key: String,
}

/// Request an agent to establish a direct connection to a client or another
/// agent.
#[cfg(any(feature = "server", feature = "agent"))]
#[derive(Serialize, Deserialize)]
pub struct DirectConnectionRequest {
    /// Instance's public address
    address: String,

    /// Randomized port
    port: u16,

    /// Randomized encryption key for the connection
    key: String,
}
