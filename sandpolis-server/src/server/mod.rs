use super::messages::GetBannerRequest;
use super::messages::GetBannerResponse;
use super::ServerLayer;
use axum::extract;
use axum::{
    extract::State,
    routing::{get, post},
    Json, Router,
};
use sandpolis_network::RequestResult;

impl ServerLayer {
    pub fn server_router() -> Router<Self> {
        Router::new().route("/banner", get(banner))
    }
}

#[axum_macros::debug_handler]
async fn banner(
    state: State<ServerLayer>,
    extract::Json(_): extract::Json<GetBannerRequest>,
) -> RequestResult<GetBannerResponse> {
    Ok(Json(GetBannerResponse::Ok(state.banner.data.clone())))
}
