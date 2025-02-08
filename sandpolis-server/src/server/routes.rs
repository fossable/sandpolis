use crate::messages::GetBannerRequest;
use crate::messages::GetBannerResponse;
use crate::ServerLayer;
use axum::extract;
use axum::{
    extract::State,
    routing::{get, post},
    Json, Router,
};
use sandpolis_network::RequestResult;

/// Return a "banner" containing server metadata.
#[axum_macros::debug_handler]
pub async fn banner(
    state: State<ServerLayer>,
    extract::Json(_): extract::Json<GetBannerRequest>,
) -> RequestResult<GetBannerResponse> {
    Ok(Json(GetBannerResponse::Ok(state.banner.data.clone())))
}
