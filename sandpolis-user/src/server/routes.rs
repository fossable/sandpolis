use anyhow::Result;
use axum::{
    extract::{self, State},
    Json,
};
use jsonwebtoken::{encode, Header};
use ring::pbkdf2;
use std::{
    num::NonZeroU32,
    time::{Duration, SystemTime},
};
use totp_rs::TOTP;
use tracing::{debug, error, info};
use validator::Validate;

use crate::{
    messages::{
        CreateUserRequest, CreateUserResponse, GetUsersRequest, GetUsersResponse, LoginRequest,
        LoginResponse,
    },
    server::PasswordData,
};
use crate::{server::Claims, UserData};
use crate::{server::KEY, UserLayer};
use sandpolis_database::Document;
use sandpolis_network::RequestResult;

#[axum_macros::debug_handler]
pub async fn login(
    state: State<UserLayer>,
    // Extension(_): Extension<GroupName>,
    extract::Json(request): extract::Json<LoginRequest>,
) -> RequestResult<LoginResponse> {
    let user: Document<UserData> = match state.users.get_document(&request.username) {
        Ok(Some(user)) => user,
        Ok(None) => {
            debug!(username = %request.username, "User does not exist");
            return Err(Json(LoginResponse::Denied));
        }
        _ => {
            error!("Failed to get user");
            return Err(Json(LoginResponse::Invalid));
        }
    };

    let password: Document<PasswordData> = match user.get_document("password") {
        Ok(Some(password)) => password,
        Ok(None) => {
            error!("Password does not exist");
            return Err(Json(LoginResponse::Denied));
        }
        _ => {
            error!("Failed to get user password");
            return Err(Json(LoginResponse::Invalid));
        }
    };

    // Check TOTP token if there is one
    if let Some(totp_url) = password.data.totp_secret {
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
        NonZeroU32::new(password.data.iterations).unwrap_or(NonZeroU32::new(1).unwrap()),
        &password.data.salt,
        request.password.0.as_bytes(),
        &password.data.hash,
    )
    .is_err()
    {
        debug!("Password check failed");
        return Err(Json(LoginResponse::Denied));
    }

    let claims = Claims {
        sub: user.data.username.to_string(),
        exp: (SystemTime::now() + Duration::from_secs(3600))
            .duration_since(SystemTime::UNIX_EPOCH)
            .unwrap()
            .as_secs() as usize,
        admin: user.data.admin,
    };

    info!(claims = ?claims, "Login succeeded");
    Ok(Json(LoginResponse::Ok(
        encode(&Header::default(), &claims, &KEY.encoding)
            .map_err(|_| Json(LoginResponse::Denied))?,
    )))
}

/// Create a new user account that can be logged in from a client instance.
#[axum_macros::debug_handler]
pub async fn create_user(
    state: State<UserLayer>,
    // Extension(_): Extension<GroupName>,
    claims: Claims,
    extract::Json(request): extract::Json<CreateUserRequest>,
) -> RequestResult<CreateUserResponse> {
    // Validate user data
    request
        .data
        .validate()
        .map_err(|_| Json(CreateUserResponse::InvalidUser))?;

    // Only admins can create other admins
    if request.data.admin && !claims.admin {
        return Err(Json(CreateUserResponse::Failed));
    }

    let password = if request.totp {
        PasswordData::new_with_totp(&request.data.username, &request.password)
            .map_err(|_| Json(CreateUserResponse::Failed))?
    } else {
        PasswordData::new(&request.password)
    };

    let user = state
        .users
        .insert_document(&request.data.username.to_string(), request.data)
        .map_err(|_| Json(CreateUserResponse::Failed))?;

    user.insert_document("password", password.clone())
        .map_err(|_| Json(CreateUserResponse::Failed))?;

    Ok(Json(CreateUserResponse::Ok {
        totp_secret: password.totp_secret,
    }))
}

#[axum_macros::debug_handler]
pub async fn get_users(
    state: State<UserLayer>,
    // Extension(_): Extension<GroupName>,
    claims: Claims,
    extract::Json(request): extract::Json<GetUsersRequest>,
) -> RequestResult<GetUsersResponse> {
    if let Some(username) = request.username {
        match state.users.get_document(&*username) {
            Ok(Some(user)) => return Ok(Json(GetUsersResponse::Ok(vec![user.data]))),
            Ok(None) => return Ok(Json(GetUsersResponse::Ok(Vec::new()))),
            Err(_) => todo!(),
        }
    }

    todo!()
}
