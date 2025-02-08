use super::LocationData;
use anyhow::Result;

pub mod ip_api;
pub mod key_cdn;

#[derive(Default)]
pub enum LocationService {
    IpApi,
    #[default]
    KeyCdn,
}

pub trait Locator {
    // TODO configurable field set?
    async fn query(&self, ip: &str) -> Result<Option<LocationData>>;
}
