/// Process-specific file descriptor number
pub fd: java.lang.Long,
/// Socket handle or inode number
pub socket: java.lang.Long,
/// Network protocol (IPv4, IPv6)
pub family: java.lang.Long,
/// Transport protocol (TCP/UDP)
pub protocol: java.lang.Long,
/// Socket local address
pub local_address: java.lang.String,
/// Socket remote address
pub remote_address: java.lang.String,
/// Socket local port
pub local_port: java.lang.Integer,
/// Socket remote port
pub remote_port: java.lang.Integer,
/// For UNIX sockets (family=AF_UNIX), the domain path
pub path: java.lang.String,
/// TCP socket state
pub state: java.lang.String,
/// The inode number of the network namespace
pub net_namespace: java.lang.String,
