use native_db::ToKey;
use native_model::Model;
use sandpolis_macros::data;

/// A port bound by a local process (listening socket).
#[data(instance)]
pub struct BoundPortData {
    /// Process (or thread) ID
    pub pid: u32,
    /// Transport layer port
    pub port: u32,
    /// Transport protocol (TCP/UDP)
    pub protocol: u32,
    /// Network protocol (IPv4, IPv6)
    pub family: u32,
    /// Specific address for bind
    pub address: String,
    /// Socket file descriptor number
    pub fd: u64,
    /// Socket handle or inode number
    pub socket: u64,
    /// Path for UNIX domain sockets
    pub path: String,
    /// The inode number of the network namespace
    pub net_namespace: u64,
}
