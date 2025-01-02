use std::net::SocketAddr;

#[derive(Clone, Serialize, Deserialize)]
pub struct TunnelStreamData {
    /// Instance hosting the listener
    pub listener_id: InstanceId,

    /// Socket to bind the listener to
    pub listener_addr: SocketAddr,

    pub repeater_iid: Vec<String>,

    pub terminator_iid: String,

    pub target_addr: SocketAddr,

    pub target_protocol: String,
}

/// Raw data flowing through a tunnel
#[derive(Serialize, Deserialize, StreamEvent)]
pub struct TunnelStreamEvent(Vec<u8>);
