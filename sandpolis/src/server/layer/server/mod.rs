use axum::{
    extract::{self, State},
    routing::{get, post},
    Json, Router,
};
use axum_macros::debug_handler;
use serde::{Deserialize, Serialize};

use crate::{
    core::{
        database::{Collection, Document},
        layer::server::{user::UserData, Banner, GetBannerRequest, GetBannerResponse},
    },
    server::ServerState,
};
use anyhow::Result;

pub mod user;

#[derive(Serialize, Deserialize, Default)]
pub struct ServerLayerData;

pub struct ServerLayer {
    banner: Document<Banner>,
    users: Collection<UserData>,
}

impl ServerLayer {
    pub fn new(document: Document<ServerLayerData>) -> Result<Self> {
        Ok(Self {
            users: document.collection("users")?,
            banner: document.document("banner")?,
        })
    }

    pub fn router() -> Router<ServerState> {
        Router::<ServerState>::new()
            .route("/banner", get(banner))
            .route("/users", get(user::get_users))
            .route("/users", post(user::create_user))
            .route("/login", post(user::login))
    }
}

#[debug_handler]
async fn banner(
    state: State<ServerState>,
    extract::Json(_): extract::Json<GetBannerRequest>,
) -> Result<Json<GetBannerResponse>, Json<GetBannerResponse>> {
    let banner = state.server.banner.data.clone();

    Ok(Json(GetBannerResponse::Ok(banner)))
}
