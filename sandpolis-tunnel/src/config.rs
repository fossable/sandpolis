//! Realm-config declaration of tunnels.
//!
//! Tunnels are configured only in the realm config (the global stratum server
//! reads it); the GUI and CLI display and monitor them but don't create them.
//! The syntax mirrors SSH local/reverse forwards: name the endpoint that binds
//! (`listener`), where it binds (`listen`), the endpoint that dials out
//! (`terminator`), and the target it dials (`target`).

use crate::{TunnelMode, TunnelProtocol};
use serde::{Deserialize, Serialize};
use std::net::SocketAddr;

#[derive(Serialize, Deserialize, Debug, Clone, Default, PartialEq)]
pub struct TunnelManagerConfig {
    pub tunnels: Vec<TunnelConfig>,
}

/// One declared tunnel.
///
/// Endpoints are named by their `InstanceId` (as shown in the GUI and by
/// `sandpolis agents list`), stored as a string so the config stays readable
/// and hand-editable.
#[derive(Serialize, Deserialize, Debug, Clone, PartialEq)]
pub struct TunnelConfig {
    /// Human-readable name for the tunnel.
    pub name: String,

    /// Instance id of the endpoint that binds the listener.
    pub listener: String,

    /// Address the listener binds, e.g. `127.0.0.1:8080`.
    pub listen: SocketAddr,

    /// Instance id of the endpoint that dials the target.
    pub terminator: String,

    /// The target the terminator dials, e.g. `10.0.0.5:80` or `db.internal:5432`.
    pub target: String,

    #[serde(default)]
    pub protocol: TunnelProtocol,

    #[serde(default)]
    pub mode: TunnelMode,
}
