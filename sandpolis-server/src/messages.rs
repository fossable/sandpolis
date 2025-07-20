use crate::ServerBanner;
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize)]
pub struct GetBannerRequest {
    /// Whether to include the banner image in the response.
    pub include_image: bool,
}

#[derive(Serialize, Deserialize)]
pub struct GetBannerResponse(pub ServerBanner);
