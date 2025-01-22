use std::io::Cursor;

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
            server::{
                group::{GroupCaCert, GroupData},
                user::UserData,
                GetBannerRequest, GetBannerResponse, ServerBanner,
            },
        },
        ClusterId, InstanceId,
    },
    server::ServerState,
};
use anyhow::Result;

pub mod group;
pub mod raft;
pub mod user;

#[derive(Serialize, Deserialize, Default)]
pub struct ServerLayerData;

pub struct ServerLayer {
    pub banner: Document<ServerBanner>,
    pub users: Collection<UserData>,
    pub groups: Collection<GroupData>,
}

impl ServerLayer {
    pub fn new(cluster_id: ClusterId, document: Document<ServerLayerData>) -> Result<Self> {
        // Create a new admin user if one doesn't exist
        let users: Collection<UserData> = document.collection("users")?;
        if users
            .documents()
            .filter_map(|user| user.ok())
            .find(|user| user.data.admin)
            .is_none()
        {
            let user = users.insert_document(
                "admin",
                UserData {
                    username: "admin".parse()?,
                    admin: true,
                    email: None,
                    phone: None,
                    expiration: None,
                },
            )?;

            let default = "test"; // TODO hash
                                  // TODO transaction
            user.insert_document("password", PasswordData::new(&default))?;
            info!(username = "admin", password = %default, "Created default admin user");
        }

        // Create default group if one doesn't exist
        let groups: Collection<GroupData> = document.collection("groups")?;
        if groups
            .documents()
            .filter_map(|group| group.ok())
            .find(|group| *group.data.name == "default")
            .is_none()
        {
            let group = groups.insert_document(
                "default",
                GroupData {
                    name: "default".parse()?,
                    owner: "admin".to_string(),
                },
            )?;

            group.insert_document("ca", GroupCaCert::new(cluster_id)?)?;
        }

        Ok(Self {
            users,
            groups,
            banner: document.document("banner")?,
        })
    }

    pub fn default_group(&self) -> Result<GroupData> {
        todo!()
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
