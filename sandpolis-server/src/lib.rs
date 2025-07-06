use anyhow::Result;
use native_db::ToKey;
use native_model::Model;
#[cfg(feature = "server")]
use sandpolis_core::RealmName;
use sandpolis_core::{ClusterId, InstanceId};
use sandpolis_database::DatabaseLayer;
use sandpolis_database::Resident;
use sandpolis_macros::data;
use sandpolis_network::NetworkLayer;
use serde::{Deserialize, Serialize};

pub mod config;

#[cfg(feature = "server")]
pub mod server;

#[cfg(feature = "client")]
pub mod client;

pub mod messages;

#[data]
pub struct ServerLayerData {}

#[derive(Clone)]
pub struct ServerLayer {
    #[cfg(feature = "server")]
    pub banner: Resident<ServerBannerData>,
    pub network: NetworkLayer,
}

impl ServerLayer {
    pub async fn new(database: DatabaseLayer, network: NetworkLayer) -> Result<Self> {
        Ok(Self {
            #[cfg(feature = "server")]
            banner: database.realm(RealmName::default()).await?.resident(())?,
            network,
        })
    }

    pub fn get_banner() -> Result<ServerBannerData> {
        todo!()
    }
}

/// Contains information about the server useful for prospective logins
#[data]
pub struct ServerBannerData {
    pub cluster_id: ClusterId,

    /// Indicates that only admin users will be allowed to login
    pub maintenance: bool,

    /// A string to display on the login screen
    pub message: Option<String>,

    /// An image to display on the login screen
    #[serde(with = "serde_bytes")]
    pub image: Option<Vec<u8>>, // TODO validate with image decoder

    /// Whether users are required to provide a second authentication mechanism
    /// on login
    pub mfa: bool,
}

/// A group is a collection of instances within the same realm.
#[data]
pub struct GroupData {
    #[secondary_key(unique)]
    pub name: String,

    pub members: Vec<InstanceId>,
}
