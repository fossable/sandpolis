use native_db::ToKey;
use native_model::Model;
use sandpolis_macros::data;

/// A single entry in the host's routing table.
#[data(instance)]
pub struct RouteData {
    /// Destination IP address
    pub destination: String,
    /// Netmask length
    pub netmask: u32,
    /// Route gateway
    pub gateway: String,
    /// Route source
    pub source: String,
    /// Flags to describe route
    pub flags: u32,
    /// Route local interface
    pub interface_id: String,
    /// Maximum Transmission Unit for the route
    pub mtu: u32,
    /// Cost of route. Lowest is preferred
    pub metric: u32,
    /// Type of route
    pub r#type: String,
    /// Max hops expected
    pub hopcount: u32,
}
