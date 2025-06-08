use native_db::*;
use native_model::{Model, native_model};
use sandpolis_core::InstanceId;
use sandpolis_database::{Data, DataIdentifier, DbTimestamp};
use sandpolis_macros::Data;
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, Clone, PartialEq, Debug, Data)]
#[native_model(id = 23, version = 1)]
#[native_db]
pub struct NetworkData {
    #[primary_key]
    pub _id: DataIdentifier,

    /// The host's hostname
    pub hostname: String,
    /// The host's fully-qualified domain name
    pub fqdn: String,
    /// The host's DNS servers
    pub dns: String,
}
