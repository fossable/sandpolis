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
            banner: database.realm(RealmName::default())?.resident(())?,
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

#[data(instance, temporal)]
pub struct LocationData {
    /// The AS name
    pub as_name: Option<String>,
    /// The numerical AS code
    pub as_code: Option<u32>,
    /// The city name
    pub city: Option<String>,
    /// The continent name
    pub continent: Option<String>,
    /// The ISO continent code
    pub continent_code: Option<String>,
    /// The country name
    pub country: Option<String>,
    /// Two-letter country code (ISO 3166-1 alpha-2)
    pub country_code: Option<String>,
    /// The currency name
    pub currency: Option<String>,
    /// The city district name
    pub district: Option<String>,
    /// The Internet Service Provider name
    pub isp: Option<String>,
    /// The approximate latitude in degrees
    pub latitude: Option<f64>,
    /// The approximate longitude in degrees
    pub longitude: Option<f64>,
    /// The metro code
    pub metro_code: Option<u32>,
    /// The organization name
    pub organization: Option<String>,
    /// The zip code
    pub postal_code: Option<String>,
    /// The region name
    pub region: Option<String>,
    /// The region short code (FIPS or ISO)
    pub region_code: Option<String>,
    /// The timezone name
    pub timezone: Option<String>,
}
