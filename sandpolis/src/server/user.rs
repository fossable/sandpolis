use anyhow::Result;
use axum::{
    extract::{self, State},
    Json,
};
use axum_macros::debug_handler;
use ring::pbkdf2;
use serde::{Deserialize, Serialize};
use std::{fmt::Display, num::NonZeroU32};
use validator::Validate;

use crate::core::database::Document;

use super::ServerState;

#[derive(Serialize, Deserialize, Validate)]
pub struct PasswordData {
    /// Number of rounds to use when hashing password
    #[validate(range(min = 1800, max = 200000))]
    pub iterations: u32,

    /// Random data used to salt the password hash
    pub salt: Vec<u8>,

    /// Password hash
    pub hash: Vec<u8>,

    /// TOTP secret token
    pub totp_secret: Option<URL>,
}

impl Display for PasswordData {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        todo!()
    }
}

#[debug_handler]
async fn login(
    state: State<ServerState>,
    extract::Json(request): extract::Json<LoginRequest>,
) -> Result<Json<LoginResponse>, StatusCode> {
    pbkdf2::verify(
        pbkdf2::PBKDF2_HMAC_SHA256,
        NonZeroU32::new(self.password.data.iterations).unwrap_or(NonZeroU32::new(1).unwrap()),
        &self.password.data.salt,
        password.as_bytes(),
        &self.password.data.hash,
    )
    .is_ok()
}

#[debug_handler]
async fn create_user(
    state: State<ServerState>,
    extract::Json(request): extract::Json<CreateUserRequest>,
) -> Result<Json<CreateUserResponse>, StatusCode> {
    todo!()
}

#[debug_handler]
async fn get_users(
    state: State<ServerState>,
    extract::Json(request): extract::Json<GetUsersRequest>,
) -> Result<Json<GetUsersResponse>, StatusCode> {
    todo!()
}
