use super::LocationData;
use anyhow::Result;

pub mod ip_api;
pub mod key_cdn;

pub trait Locator {
    // TODO configurable field set?
    async fn query(&self, ip: &str) -> Result<Option<LocationData>>;
}
