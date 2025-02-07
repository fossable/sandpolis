use super::ServerBanner;
use serde::{Deserialize, Serialize};

#[cfg(any(feature = "server", feature = "client"))]
#[derive(Serialize, Deserialize)]
pub struct GetBannerRequest;

#[cfg(any(feature = "server", feature = "client"))]
#[derive(Serialize, Deserialize)]
pub enum GetBannerResponse {
    Ok(ServerBanner),
}
