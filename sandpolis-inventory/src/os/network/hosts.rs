use native_db::ToKey;
use native_model::Model;
use sandpolis_macros::data;

/// An entry in the host's static hostname table (e.g. `/etc/hosts`).
#[data(instance)]
pub struct HostEntryData {
    /// IP address mapping
    pub address: String,
    /// Raw hosts mapping
    pub hostnames: String,
}
