use super::LocationData;
use anyhow::Result;

pub mod ip_api;

pub trait Locator {
    async fn query(&self, ip: &str) -> Result<Option<LocationData>>;
}
