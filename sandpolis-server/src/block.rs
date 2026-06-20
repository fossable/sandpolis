use axum::extract::{ConnectInfo, Request, State};
use axum::http::StatusCode;
use axum::middleware::Next;
use axum::response::{IntoResponse, Response};
use std::collections::HashSet;
use std::net::{IpAddr, SocketAddr};
use std::sync::{Arc, RwLock};
use tracing::debug;

/// A set of IP addresses that are denied access to the server. Cheaply
/// cloneable and safe to share across the middleware stack.
#[derive(Clone, Default)]
pub struct IpBlockList(Arc<RwLock<HashSet<IpAddr>>>);

impl IpBlockList {
    /// Create a block list seeded with the given addresses.
    pub fn new(addresses: impl IntoIterator<Item = IpAddr>) -> Self {
        Self(Arc::new(RwLock::new(addresses.into_iter().collect())))
    }

    /// Deny access to the given address.
    pub fn block(&self, address: IpAddr) {
        self.0.write().unwrap().insert(address);
    }

    /// Restore access for the given address.
    pub fn unblock(&self, address: &IpAddr) {
        self.0.write().unwrap().remove(address);
    }

    /// Whether the given address is currently blocked.
    pub fn is_blocked(&self, address: &IpAddr) -> bool {
        self.0.read().unwrap().contains(address)
    }
}

/// Reject requests originating from a blocked IP address with `403 Forbidden`
/// before they reach authentication or any route handler.
pub async fn block_middleware(
    State(blocklist): State<IpBlockList>,
    ConnectInfo(peer): ConnectInfo<SocketAddr>,
    request: Request,
    next: Next,
) -> Response {
    if blocklist.is_blocked(&peer.ip()) {
        debug!(peer = %peer.ip(), "Rejecting request from blocked IP");
        return StatusCode::FORBIDDEN.into_response();
    }
    next.run(request).await
}

#[cfg(test)]
mod test {
    use super::*;

    #[test]
    fn block_and_unblock() {
        let ip: IpAddr = "10.0.0.5".parse().unwrap();
        let list = IpBlockList::new([ip]);
        assert!(list.is_blocked(&ip));

        list.unblock(&ip);
        assert!(!list.is_blocked(&ip));

        list.block(ip);
        assert!(list.is_blocked(&ip));
    }
}
