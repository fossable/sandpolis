//! ## Streams
//!
//! Many operations require real-time data for a short-lived or long-lived session.
//!
//! All streams have a _source_ and a _sink_ and can exist between any two instances
//! (a stream where the source and sink reside on the same instance is called a
//! _local stream_). The source's purpose is to produce _stream events_ at whatever
//! frequency is appropriate for the use-case and the sink's purpose is to consume
//! those stream events.
//!
//! ### Stream Transport
//!
//! Stream transport is handled by secure websockets and uses binary messages rather
//! than JSON.
//!
//! ### Direct Streams
//!
//! For high-volume or low-latency streams, a direct connection may be preferable to
//! a connection through a server. In these cases, the server will first attempt to
//! coordinate a websocket connection between the two instances using the typical
//! "hole-punching" strategy. If a direct connection cannot be established, the stream
//! falls back to indirect mode.
//!
//! Direct websocket connections can be coordinated only between a client and agent
//! or between two agents.

use axum::extract::ws::Message;
use serde::{Deserialize, Serialize};

/// Unique identifier for a stream within an `InstanceConnection`.
/// Upper 32 bits = type tag (hash of crate + struct name), lower 32 bits = random.
pub type StreamId = u64;

/// Extract the type tag from a stream ID.
pub fn stream_type_tag(id: StreamId) -> u32 {
    (id >> 32) as u32
}

/// Message that gets sent over an `InstanceConnection`.
#[derive(Serialize, Deserialize)]
pub struct StreamMessage {
    pub stream_id: StreamId,
    pub payload: Vec<u8>,
}

/// Implemented by stream types to generate unique IDs.
/// Use `#[derive(Stream)]` from `sandpolis_macros` to implement this.
pub trait Stream {
    /// Generate a new unique stream ID for this stream type.
    fn generate_id() -> StreamId;
}

/// Handles incoming messages for an active stream instance.
pub trait StreamHandler: Send + Sync + 'static {
    /// Called when the stream receives a message from the remote peer.
    fn on_receive(&self, message: StreamMessage);
}

pub fn event_to_message<T>(event: &T) -> Message
where
    T: Serialize,
{
    Message::Binary(axum::body::Bytes::from(serde_cbor::to_vec(&event).unwrap()))
}
