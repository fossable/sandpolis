use crate::ServerBanner;
use crate::ServerLayer;
use axum::extract;
use axum::{Json, Router, extract::State};
use native_db::ToKey;
use native_model::Model;
use sandpolis_macros::data;
use sandpolis_network::RequestResult;
use serde::Deserialize;
use serde::Serialize;

#[data]
#[derive(Default)]
pub struct ServerBannerData {
    inner: ServerBanner,
}

#[derive(Serialize, Deserialize)]
pub struct GetBannerRequest {
    /// Whether to include the banner image in the response.
    pub include_image: bool,
}

#[derive(Serialize, Deserialize)]
pub struct GetBannerResponse(pub ServerBanner);

/// Return a "banner" containing server metadata.
#[axum_macros::debug_handler]
pub async fn banner(
    state: State<ServerLayer>,
    extract::Json(_): extract::Json<GetBannerRequest>,
) -> RequestResult<GetBannerResponse> {
    Ok(Json(GetBannerResponse(state.banner.read().inner.clone())))
}
