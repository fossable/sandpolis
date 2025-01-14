use axum::{
    extract::{self, State},
    routing::{get, post},
    Json, Router,
};
use axum_macros::debug_handler;
use serde::{Deserialize, Serialize};
use tracing::info;
use user::PasswordData;

use crate::{
    core::{
        database::{Collection, Document},
        layer::{
            network::RequestResult,
            server::{user::UserData, Banner, GetBannerRequest, GetBannerResponse},
        },
    },
    server::ServerState,
};
use anyhow::Result;

pub mod group;
pub mod user;

#[derive(Serialize, Deserialize, Default)]
pub struct ServerLayerData;

pub struct ServerLayer {
    banner: Document<Banner>,
    users: Collection<UserData>,
}

impl ServerLayer {
    pub fn new(document: Document<ServerLayerData>) -> Result<Self> {
        // Create a new admin user if one doesn't exist
        let users: Collection<UserData> = document.collection("users")?;
        if users
            .documents()
            .filter_map(|user| user.ok())
            .find(|(_, user)| user.admin)
            .is_none()
        {
            let user = users.insert_document(
                "admin",
                UserData {
                    username: "admin".to_string(),
                    admin: true,
                    email: None,
                    phone: None,
                    expiration: None,
                },
            )?;

            let default = "test"; // TODO hash
            user.insert_document("password", PasswordData::new(&default))?;
            info!(username = "admin", password = %default, "Created default admin user");
        }

        Ok(Self {
            users,
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
) -> RequestResult<GetBannerResponse> {
    let banner = state.server.banner.data.clone();

    Ok(Json(GetBannerResponse::Ok(banner)))
}
