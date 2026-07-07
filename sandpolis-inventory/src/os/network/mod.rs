use native_db::ToKey;
use native_model::Model;
use sandpolis_macros::data;

pub mod arp;
pub mod bound_port;
pub mod hosts;
pub mod route;

#[data(instance)]
pub struct NetworkData {
    /// The host's hostname
    pub hostname: String,
    /// The host's fully-qualified domain name
    pub fqdn: String,
    /// The host's DNS servers
    pub dns: String,
}
