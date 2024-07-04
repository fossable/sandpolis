
// Ping response.
enum GetPingResponse {
    PING_OK = 0;
}

// Request the server for a new direct connection.
message RQ_DirectConnection {

    // The requested node
    int32 sid = 1;

    // An optional listener port. If specified, the requested node will attempt
    // a connection on this port. Otherwise, the server will coordinate the connection.
    int32 port = 3;
}

// Request that the recieving instance establish a new connection to the given host.
message RQ_CoordinateConnection {

    // The host IP address
    string host = 1;

    // The port
    int32 port = 2;

    // The transport protocol type
    string transport = 3;

    // The initial encryption key for the new connection.
    bytes encryption_key = 4;
}

// Indicates that some node in the network has changed in connection status.
message EV_NetworkChange {
    message NodeAdded {
        int32 sid = 1;
        int32 parent = 2;
    }
    repeated NodeAdded node_added = 1;

    message NodeRemoved {
        int32 sid = 1;
    }
    repeated NodeRemoved node_removed = 2;

    message LinkAdded {
        int32 cvid1 = 1;
        int32 cvid2 = 2;
    }
    repeated LinkAdded connection_added = 3;

    message LinkRemoved {
        int32 cvid1 = 1;
        int32 cvid2 = 2;
    }
    repeated LinkRemoved connection_removed = 4;

}
