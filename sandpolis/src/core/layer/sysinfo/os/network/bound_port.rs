/// Process (or thread) ID
pub pid: java.lang.Integer,
/// Transport layer port
pub port: java.lang.Integer,
/// Transport protocol (TCP/UDP)
pub protocol: java.lang.Integer,
/// Network protocol (IPv4, IPv6)
pub family: java.lang.Integer,
/// Specific address for bind
pub address: java.lang.String,
/// Socket file descriptor number
pub fd: java.lang.Long,
/// Socket handle or inode number
pub socket: java.lang.Long,
/// Path for UNIX domain sockets
pub path: java.lang.Long,
/// The inode number of the network namespace
pub net_namespace: java.lang.Long,
