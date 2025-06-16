use anyhow::Result;
use native_db::*;
use native_model::{Model, native_model};
use sandpolis_core::ClusterId;
#[cfg(feature = "server")]
use sandpolis_database::Resident;
use sandpolis_database::{Data, DataIdentifier, DatabaseLayer};
use sandpolis_macros::Data;
use sandpolis_network::{NetworkLayer, ServerAddress};
use serde::{Deserialize, Serialize};

pub mod config;

#[cfg(feature = "server")]
pub mod server;

#[cfg(feature = "client")]
pub mod client;

pub mod messages;

#[derive(Serialize, Deserialize, Clone, Default, PartialEq, Debug, Data)]
#[native_model(id = 25, version = 1)]
#[native_db]
pub struct ServerLayerData {
    #[primary_key]
    pub _id: DataIdentifier,
}

#[derive(Clone)]
pub struct ServerLayer {
    #[cfg(feature = "server")]
    pub banner: Resident<ServerBannerData>,
    pub network: NetworkLayer,
}

impl ServerLayer {
    pub fn new(database: DatabaseLayer, network: NetworkLayer) -> Result<Self> {
        Ok(Self {
            #[cfg(feature = "server")]
            banner: if let Some(document) = data.get_document("/banner")? {
                document
            } else {
                // Load banner from another server if there is one
                // TODO

                // Create a new banner
                data.document("/banner")?
            },
            network,
        })
    }

    pub fn get_banner() -> Result<ServerBannerData> {
        todo!()
    }
}

/// Contains information about the server useful for prospective logins
#[derive(Serialize, Deserialize, Clone, Default)]
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
#[derive(Serialize, Deserialize, Clone, Default, PartialEq, Debug, Data)]
#[native_model(id = 26, version = 1)]
#[native_db]
pub struct GroupData {
    #[primary_key]
    pub _id: DataIdentifier,

    #[secondary_key(unique)]
    pub name: String,

    pub members: Vec<InstanceId>,
}
