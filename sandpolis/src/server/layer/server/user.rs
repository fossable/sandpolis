use anyhow::Result;
use axum::{
    extract::{self, FromRequestParts, State},
    http::{request::Parts, StatusCode},
    Json, RequestPartsExt,
};
use axum_extra::{
    headers::{authorization::Bearer, Authorization},
    TypedHeader,
};
use axum_macros::debug_handler;
use jsonwebtoken::{decode, encode, DecodingKey, EncodingKey, Header, Validation};
use rand::Rng;
use ring::pbkdf2;
use serde::{Deserialize, Serialize};
use std::sync::LazyLock;
use std::{
    fmt::{Debug, Display},
    num::NonZeroU32,
};
use totp_rs::{Secret, TOTP};
use tracing::{debug, error};
use validator::Validate;

use crate::core::{
    database::Document,
    layer::server::user::{
        CreateUserRequest, CreateUserResponse, GetUsersRequest, GetUsersResponse, LoginRequest,
        LoginResponse, UserData,
    },
};

use super::ServerState;

static KEY: LazyLock<ServerKey> = LazyLock::new(|| ServerKey::new());

struct ServerKey {
    encoding: EncodingKey,
    decoding: DecodingKey,
}

impl ServerKey {
    fn new() -> Self {
        let secret = rand::thread_rng().gen::<[u8; 32]>().to_vec();
        Self {
            encoding: EncodingKey::from_secret(&secret),
            decoding: DecodingKey::from_secret(&secret),
        }
    }
}

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
    pub totp_secret: Option<String>,
}

impl Display for PasswordData {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        todo!()
    }
}

impl Debug for PasswordData {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("PasswordData")
            .field("iterations", &self.iterations)
            .finish()
    }
}

#[derive(Debug, Serialize, Deserialize)]
pub struct Claims {
    sub: String,
    exp: usize,
}

impl<S> FromRequestParts<S> for Claims
where
    S: Send + Sync,
{
    type Rejection = StatusCode;

    async fn from_request_parts(parts: &mut Parts, _state: &S) -> Result<Self, Self::Rejection> {
        // Extract the token from the authorization header
        let TypedHeader(Authorization(bearer)) = parts
            .extract::<TypedHeader<Authorization<Bearer>>>()
            .await
            .map_err(|_| StatusCode::BAD_REQUEST)?;

        let token_data = decode::<Claims>(bearer.token(), &KEY.decoding, &Validation::default())
            .map_err(|_| StatusCode::FORBIDDEN)?;

        Ok(token_data.claims)
    }
}

#[debug_handler]
pub async fn login(
    state: State<ServerState>,
    extract::Json(request): extract::Json<LoginRequest>,
) -> Result<Json<LoginResponse>, Json<LoginResponse>> {
    let user: Document<UserData> = match state.server.users.get_document(&request.username) {
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
    if pbkdf2::verify(
        pbkdf2::PBKDF2_HMAC_SHA256,
        NonZeroU32::new(password.data.iterations).unwrap_or(NonZeroU32::new(1).unwrap()),
        &password.data.salt,
        request.password.as_bytes(),
        &password.data.hash,
    )
    .is_err()
    {
        debug!("Password check failed");
        return Err(Json(LoginResponse::Denied));
    }

    let claims = Claims {
        sub: user.data.username.clone(),
        exp: todo!(),
    };
    Ok(Json(LoginResponse::Ok(
        encode(&Header::default(), &claims, &KEY.encoding)
            .map_err(|_| Json(LoginResponse::Denied))?,
    )))
}

#[debug_handler]
pub async fn create_user(
    state: State<ServerState>,
    extract::Json(request): extract::Json<CreateUserRequest>,
) -> Result<Json<CreateUserResponse>, Json<CreateUserResponse>> {
    let mut password = PasswordData {
        iterations: 15000,
        salt: rand::thread_rng().gen::<[u8; 32]>().to_vec(),
        hash: Vec::new(),
        totp_secret: if request.totp {
            Some(
                TOTP::new(
                    totp_rs::Algorithm::SHA1,
                    6,
                    1,
                    30,
                    Secret::default().to_bytes().unwrap(),
                    Some("Sandpolis".to_string()),
                    "".to_string(),
                )
                .unwrap()
                .get_url(),
            )
        } else {
            None
        },
    };
    pbkdf2::derive(
        pbkdf2::PBKDF2_HMAC_SHA256,
        NonZeroU32::new(password.iterations).expect("nonzero"),
        &password.salt,
        request.password.as_bytes(),
        &mut password.hash,
    );

    // TODO atomic insert
    let users = &state.server.users;
    // state.server.users.document(request.data.username);

    Ok(Json(CreateUserResponse::Ok { totp_secret: None }))
}

#[debug_handler]
pub async fn get_users(
    state: State<ServerState>,
    claims: Claims,
    extract::Json(request): extract::Json<GetUsersRequest>,
) -> Result<Json<GetUsersResponse>, Json<GetUsersResponse>> {
    let users = &state.server.users;

    if let Some(username) = request.username {
        match users.get_document(&username) {
            Ok(Some(user)) => return Ok(Json(GetUsersResponse::Ok(vec![user.data]))),
            Ok(None) => return Ok(Json(GetUsersResponse::Ok(Vec::new()))),
            Err(_) => todo!(),
        }
    }

    todo!()
}
