use native_db::ToKey;
use native_model::Model;
use sandpolis_macros::data;

/// A socket opened by a process.
#[data(instance)]
pub struct SocketData {
    /// Process-specific file descriptor number
    pub fd: u64,
    /// Socket handle or inode number
    pub socket: u64,
    /// Network protocol (IPv4, IPv6)
    pub family: u64,
    /// Transport protocol (TCP/UDP)
    pub protocol: u64,
    /// Socket local address
    pub local_address: String,
    /// Socket remote address
    pub remote_address: String,
    /// Socket local port
    pub local_port: u32,
    /// Socket remote port
    pub remote_port: u32,
    /// For UNIX sockets (family=AF_UNIX), the domain path
    pub path: String,
    /// TCP socket state
    pub state: String,
    /// The inode number of the network namespace
    pub net_namespace: String,
}
