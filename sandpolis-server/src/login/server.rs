use crate::UserLayer;
use crate::{UserData, server::Claims};
use crate::{
    messages::{
        CreateUserRequest, CreateUserResponse, GetUsersRequest, GetUsersResponse, LoginRequest,
        LoginResponse,
    },
    server::PasswordData,
};
use anyhow::Result;
use aws_lc_rs::pbkdf2;
use axum::{
    Json,
    extract::{self, State},
};
use axum_extra::TypedHeader;
use futures::stream::StreamExt;
use jsonwebtoken::{Header, encode};
use sandpolis_instance::network::RequestResult;
use sandpolis_instance::realm::RealmName;
use std::time::{Duration, SystemTime};
use totp_rs::TOTP;
use tracing::{debug, error, info};
use validator::Validate;

#[axum_macros::debug_handler]
pub async fn login(
    state: State<UserLayer>,
    TypedHeader(realm): TypedHeader<RealmName>,
    extract::Json(request): extract::Json<LoginRequest>,
) -> RequestResult<LoginResponse> {
    request
        .validate()
        .map_err(|_| Json(LoginResponse::Invalid))?;

    let Ok(user) = state.user(&request.username).await else {
        debug!(username = %request.username, "User does not exist");
        return Err(Json(LoginResponse::Denied));
    };

    let Ok(password) = state.password(request.username.clone()).await else {
        error!("Failed to get user password");
        return Err(Json(LoginResponse::Invalid));
    };

    // Check that requested token lifetime is within valid range
    // TODO
    let lifetime = request.lifetime.unwrap_or(Duration::new(1, 0));

    // Check TOTP token if there is one
    if let Some(totp_url) = password.totp_secret.as_ref() {
        if request.totp_token.unwrap_or(String::new())
            != TOTP::from_url(totp_url)
                .unwrap()
                .generate_current()
                .unwrap()
        {
            debug!("TOTP check failed");
            return Err(Json(LoginResponse::Denied));
        }
    }

    // Check password
    // TODO argon2
    if pbkdf2::verify(
        pbkdf2::PBKDF2_HMAC_SHA256,
        std::num::NonZero::new(password.iterations).unwrap(),
        &password.salt,
        request.password.0.as_bytes(),
        &password.hash,
    )
    .is_err()
    {
        debug!("Password check failed");
        return Err(Json(LoginResponse::Denied));
    }

    let claims = Claims {
        sub: user.username.clone(),
        exp: (SystemTime::now() + lifetime)
            .duration_since(SystemTime::UNIX_EPOCH)
            .unwrap()
            .as_secs() as usize,
        admin: user.admin,
        realm,
    };

    info!(claims = ?claims, "Login succeeded");
    Ok(Json(LoginResponse::Ok(
        state
            .new_token(claims)
            .map_err(|_| Json(LoginResponse::Denied))?,
    )))
}
