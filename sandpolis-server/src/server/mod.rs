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

impl ServerLayer {
    pub fn new(cluster_id: ClusterId, document: Document<ServerLayerData>) -> Result<Self> {
        // Create a new admin user if one doesn't exist
        let users: Collection<UserData> = document.collection("users")?;

        // Create default group if one doesn't exist
        let groups: Collection<GroupData> = document.collection("groups")?;

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
