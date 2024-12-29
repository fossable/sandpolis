use crate::core::InstanceId;

pub struct NetworkLayer {
    db: sled::Tree,
}

struct PingRequest;

enum PingResponse {
    Ok,
}

impl NetworkLayer {
    pub fn ping(&self, id: InstanceId) {}
}

// Request the server for a new direct connection.
// message RQ_DirectConnection {

//     // The requested node
//     int32 sid = 1;

//     // An optional listener port. If specified, the requested node will attempt
//     // a connection on this port. Otherwise, the server will coordinate the connection.
//     int32 port = 3;
// }

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
