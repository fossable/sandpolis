//! Wire types for the tunnel data stream.
//!
//! A tunnel is bridged by the orchestrating server. The server opens one
//! [`TunnelStream`] toward the *listener* endpoint and one toward the
//! *terminator* endpoint, then copies bytes between them keyed by a logical
//! connection id. The server is always the requester; each endpoint is a
//! responder. The same stream type carries both roles — the initial
//! [`TunnelStreamRequest::Open`] tells the endpoint which one it is.
//!
//! For TCP a logical connection is one accepted socket. For UDP it is one
//! source address (a "session"), with the same message flow.

use crate::{TunnelProtocol, TunnelRole};
use serde::{Deserialize, Serialize};

/// Server -> endpoint messages.
#[derive(Serialize, Deserialize, Debug)]
pub enum TunnelStreamRequest {
    /// First message: assume this role for the stream.
    Open { role: TunnelRole, protocol: TunnelProtocol },
    /// Terminator: open a new outbound connection to the target for `conn`.
    Connect { conn: u64 },
    /// Bytes for a logical connection.
    Data { conn: u64, bytes: Vec<u8> },
    /// Tear down a logical connection.
    Close { conn: u64 },
}

/// Endpoint -> server messages.
#[derive(Serialize, Deserialize, Debug)]
pub enum TunnelStreamResponse {
    /// The endpoint bound (listener) or is ready to dial (terminator).
    Ready,
    /// Setting up the endpoint failed fatally (e.g. the listener couldn't bind).
    Error { message: String },
    /// Listener: a new inbound connection/session was accepted.
    Accepted { conn: u64, peer: String },
    /// Terminator: the outbound connection to the target is established.
    Connected { conn: u64 },
    /// Terminator: dialing the target failed.
    ConnectFailed { conn: u64, message: String },
    /// Bytes for a logical connection.
    Data { conn: u64, bytes: Vec<u8> },
    /// A logical connection closed on this endpoint's side.
    Closed { conn: u64 },
}
