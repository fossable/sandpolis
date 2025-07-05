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
//!
//! ### Stream Multicasting
//!
//! Stream sources can push events to more than one sink simultaneously. This is
//! called multicasting and can save bandwidth in situations where multiple users
//! request the same resource at the same time. For exmaple, if more than one user
//! requests a desktop stream on an agent simultaneously, the agent only needs to
//! produce the data once and the server will duplicate it.
//!
//! Direct streams cannot be multicast.

use axum::extract::ws::Message;
use serde::Serialize;

pub fn event_to_message<T>(event: &T) -> Message
where
    T: Serialize,
{
    Message::Binary(axum::body::Bytes::from(serde_cbor::to_vec(&event).unwrap()))
}

pub enum DataStreamDirection {
    Upstream,
    Downstream,
    Bidirectional,
}

pub struct DataStreamRequest {
    pub permanent: bool,

    // string oid = 3;
    // repeated string whitelist = 4;
    pub direction: DataStreamDirection,
    pub update_period: Option<u64>,
}

enum DataStreamResponse {
    Ok(u64),
    Invalid,
    Failed,
}

// message EV_STStreamData {

//     enum ValueType {
//         BYTES = 0;
//         BYTES_ARRAY = 1;
//         STRING = 2;
//         STRING_ARRAY = 3;
//         INTEGER = 4;
//         INTEGER_ARRAY = 5;
//         LONG = 6;
//         LONG_ARRAY = 7;
//         BOOLEAN = 8;
//         BOOLEAN_ARRAY = 9;
//         DOUBLE = 10;
//         DOUBLE_ARRAY = 11;
//         OS_TYPE = 12;
//         OS_TYPE_ARRAY = 13;
//         INSTANCE_TYPE = 14;
//         INSTANCE_TYPE_ARRAY = 15;
//         INSTANCE_FLAVOR = 16;
//         INSTANCE_FLAVOR_ARRAY = 17;
//     }

//     // The object's relative OID
//     string oid = 1;

//     // Whether the object corresponding to the OID was removed
//     bool removed = 2;

//     // The attribute value type
//     ValueType value_type = 3;

//     // The timestamp associated with the attribute value
//     int64 timestamp = 4;

//     bytes           bytes          = 5;
//     repeated bytes  bytes_array    = 6;
//     string          string         = 7;
//     repeated string string_array   = 8;
//     int32           integer        = 9;
//     repeated int32  integer_array  = 10;
//     int64           long           = 11;
//     repeated int64  long_array     = 12;
//     bool            boolean        = 13;
//     repeated bool   boolean_array  = 14;
//     double          double         = 15;
//     repeated double double_array   = 16;
// }

// message RQ_StopStream {

//     // The stream ID of the stream to stop
//     int32 id = 1;
// }

// enum RS_StopStream {
//     STOP_STREAM_OK = 0;
//     STOP_STREAM_INVALID = 1;
// }
