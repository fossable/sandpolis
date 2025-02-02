use anyhow::Result;
use axum::{
    extract::{self, FromRequestParts, State},
    http::{request::Parts, StatusCode},
    Extension, Json, RequestPartsExt,
};
use axum_extra::{
    headers::{authorization::Bearer, Authorization},
    TypedHeader,
};
use axum_macros::debug_handler;
use jsonwebtoken::{decode, encode, DecodingKey, EncodingKey, Header, Validation};
use rand::Rng;
use ring::pbkdf2;
use sandpolis_database::Collection;
use serde::{Deserialize, Serialize};
use std::{
    fmt::{Debug, Display},
    num::NonZeroU32,
};
use std::{
    sync::LazyLock,
    time::{Duration, SystemTime},
};
use totp_rs::{Secret, TOTP};
use tracing::{debug, error, info};
use validator::Validate;

use super::{
    CreateUserRequest, CreateUserResponse, GetUsersRequest, GetUsersResponse, LoginRequest,
    LoginResponse, UserData,
};
use crate::core::layer::{network::RequestResult, server::group::GroupName};
use sandpolis_database::{Database, Document};
use sandpolis_server::server::ServerState;

#[derive(Clone)]
pub struct UserState {
    users: Collection<UserData>,
}

impl UserState {
    pub fn new(db: Database) -> Result<Self> {
        let users = db.collection("/server/users")?;

        // Create an admin user if one doesn't exist already
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
        Ok(Self { users })
    }
}

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

#[derive(Serialize, Deserialize, Validate, Clone)]
pub struct PasswordData {
    /// Number of rounds to use when hashing password
    #[validate(range(min = 4284, max = 200000))]
    pub iterations: u32,

    /// Random data used to salt the password hash
    pub salt: Vec<u8>,

    /// Password hash
    pub hash: Vec<u8>,

    /// TOTP secret token
    pub totp_secret: Option<String>,
}

impl PasswordData {
    /// Create a new password without a TOTP.
    pub fn new(password: &str) -> Self {
        let mut data = PasswordData {
            iterations: 15000,
            salt: rand::thread_rng().gen::<[u8; 32]>().to_vec(),
            hash: vec![0u8; ring::digest::SHA256_OUTPUT_LEN],
            totp_secret: None,
        };
        pbkdf2::derive(
            pbkdf2::PBKDF2_HMAC_SHA256,
            NonZeroU32::new(data.iterations).expect("nonzero"),
            &data.salt,
            password.as_bytes(),
            &mut data.hash,
        );
        data
    }

    /// Create a new password with a TOTP.
    pub fn new_with_totp(username: &str, password: &str) -> Result<Self> {
        let mut data = PasswordData {
            iterations: 15000,
            salt: rand::thread_rng().gen::<[u8; 32]>().to_vec(),
            hash: Vec::new(),
            totp_secret: Some(
                TOTP::new(
                    totp_rs::Algorithm::SHA1,
                    6,
                    1,
                    30,
                    Secret::default().to_bytes()?,
                    Some("Sandpolis".to_string()),
                    username.to_string(),
                )?
                .get_url(),
            ),
        };
        pbkdf2::derive(
            pbkdf2::PBKDF2_HMAC_SHA256,
            NonZeroU32::new(data.iterations).expect("nonzero"),
            &data.salt,
            password.as_bytes(),
            &mut data.hash,
        );
        Ok(data)
    }
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
            .field("salt", &self.salt)
            .finish()
    }
}

#[derive(Debug, Serialize, Deserialize)]
pub struct Claims {
    /// Username
    sub: String,

    /// Claim expiration
    exp: usize,

    /// Whether the user is an admin
    admin: bool,
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
    Extension(_): Extension<GroupName>,
    extract::Json(request): extract::Json<LoginRequest>,
) -> RequestResult<LoginResponse> {
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
        sub: user.data.username.clone(),
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

#[debug_handler]
pub async fn create_user(
    state: State<ServerState>,
    Extension(_): Extension<GroupName>,
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
        .server
        .users
        .insert_document(&request.data.username.clone(), request.data)
        .map_err(|_| Json(CreateUserResponse::Failed))?;

    user.insert_document("password", password.clone())
        .map_err(|_| Json(CreateUserResponse::Failed))?;

    Ok(Json(CreateUserResponse::Ok {
        totp_secret: password.totp_secret,
    }))
}

#[debug_handler]
pub async fn get_users(
    state: State<UserState>,
    Extension(_): Extension<GroupName>,
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
