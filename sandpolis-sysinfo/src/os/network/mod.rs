use native_db::ToKey;
use native_model::Model;
use sandpolis_core::InstanceId;
use sandpolis_database::{Data, DataIdentifier, DbTimestamp};
use sandpolis_macros::data;
use serde::{Deserialize, Serialize};

#[data]
pub struct NetworkData {
    /// The host's hostname
    pub hostname: String,
    /// The host's fully-qualified domain name
    pub fqdn: String,
    /// The host's DNS servers
    pub dns: String,
}
