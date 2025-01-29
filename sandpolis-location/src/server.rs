use anyhow::Result;

use crate::core::layer::location::LocationData;

pub trait Geolocator {
    async fn query(ip: &str) -> Result<LocationData>;
}
