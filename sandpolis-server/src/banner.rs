use crate::ServerBanner;
#[cfg(feature = "server")]
use crate::ServerLayer;
#[cfg(feature = "server")]
use axum::{Json, extract, extract::State};
use native_db::ToKey;
use native_model::Model;
#[cfg(feature = "server")]
use sandpolis_instance::network::RequestResult;
use sandpolis_macros::data;
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
#[cfg(feature = "server")]
#[axum_macros::debug_handler]
pub async fn get_banner(
    state: State<ServerLayer>,
    extract::Json(_): extract::Json<GetBannerRequest>,
) -> RequestResult<GetBannerResponse> {
    Ok(Json(GetBannerResponse(state.banner.read().inner.clone())))
}
