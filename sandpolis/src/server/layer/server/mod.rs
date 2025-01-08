use axum::{
    extract::{self, State},
    Json,
};
use axum_macros::debug_handler;

use crate::{
    core::{
        database::{Collection, Document},
        layer::server::{Banner, GetBannerRequest, GetBannerResponse},
    },
    server::ServerState,
};

pub mod user;

pub struct ServerLayer {
    banner: Document<Banner>,
    users: Collection<UserData>,
}

#[debug_handler]
async fn banner(
    state: State<ServerState>,
    extract::Json(_): extract::Json<GetBannerRequest>,
) -> Result<Json<GetBannerResponse>, Json<GetBannerResponse>> {
    let banner = state.server.banner.data.clone();

    Ok(Json(GetBannerResponse::Ok(banner)))
}
