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
use axum::{
    Json,
    extract::{self, State},
};
use axum_extra::TypedHeader;
use futures::stream::StreamExt;
use jsonwebtoken::{Header, encode};
use ring::pbkdf2;
use sandpolis_core::RealmName;
use sandpolis_network::RequestResult;
use std::{
    num::NonZeroU32,
    time::{Duration, SystemTime},
};
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
        NonZeroU32::new(password.iterations).unwrap_or(NonZeroU32::new(1).unwrap()),
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

/// Create a new user
#[axum_macros::debug_handler]
pub async fn create_user(
    state: State<UserLayer>,
    claims: Claims,
    extract::Json(request): extract::Json<CreateUserRequest>,
) -> RequestResult<CreateUserResponse> {
    request
        .validate()
        .map_err(|_| Json(CreateUserResponse::InvalidUser))?;

    // Only admins can create other admins
    if request.data.admin && !claims.admin {
        return Err(Json(CreateUserResponse::Failed));
    }

    // Create new password
    let password = if request.totp {
        state
            .new_password_with_totp(request.data.username.clone(), &request.password)
            .await
            .map_err(|_| Json(CreateUserResponse::Failed))?
    } else {
        state
            .new_password(request.data.username.clone(), &request.password)
            .await
            .map_err(|_| Json(CreateUserResponse::Failed))?
    };

    // Add new user
    state
        .users
        .push(request.data)
        .map_err(|_| Json(CreateUserResponse::Failed))?;

    Ok(Json(CreateUserResponse::Ok {
        totp_secret: password.totp_secret,
    }))
}

#[axum_macros::debug_handler]
pub async fn get_users(
    state: State<UserLayer>,
    claims: Claims,
    extract::Json(request): extract::Json<GetUsersRequest>,
) -> RequestResult<GetUsersResponse> {
    if let Some(username) = request.username {
        // match state.users.get_document(&*username) {
        //     Ok(Some(user)) => return
        // Ok(Json(GetUsersResponse::Ok(vec![user.data]))),     Ok(None)
        // => return Ok(Json(GetUsersResponse::Ok(Vec::new()))),
        //     Err(_) => todo!(),
        // }
    }

    todo!()
}
