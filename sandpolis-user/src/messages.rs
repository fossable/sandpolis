use std::time::Duration;

use crate::{ClientAuthToken, LoginPassword, UserData};
use sandpolis_core::UserName;
use serde::{Deserialize, Serialize};
use validator::Validate;

/// Create a new user account.
#[derive(Serialize, Deserialize, Validate)]
pub struct CreateUserRequest {
    // TODO inline
    pub data: UserData,

    /// Password as unsalted hash
    pub password: String,

    /// Whether a TOTP secret should be generated
    pub totp: bool,
}

#[derive(Serialize, Deserialize)]
pub enum CreateUserResponse {
    Ok {
        /// TOTP secret URL
        totp_secret: Option<String>,
    },
    Failed,
    InvalidUser,
}

#[derive(Serialize, Deserialize)]
pub struct GetUsersRequest {
    /// Search by username prefix
    pub username: Option<UserName>,

    /// Search by email prefix
    pub email: Option<String>,
}

#[derive(Serialize, Deserialize)]
pub enum GetUsersResponse {
    Ok(Vec<UserData>),
    PermissionDenied,
}

/// Update an existing user account.
#[derive(Serialize, Deserialize, Validate)]
pub struct UpdateUserRequest {
    /// User to edit
    pub username: UserName,

    /// New password
    pub password: Option<String>,

    /// New email
    pub email: Option<String>,

    /// New phone number
    pub phone: Option<String>,

    /// New expiration timestamp
    pub expiration: Option<u64>,
}

#[derive(Serialize, Deserialize)]
pub enum UpdateUserResponse {
    Ok,

    /// The requested user does not exist
    NotFound,
}

/// Request a login from the server
#[derive(Serialize, Deserialize, Debug, Clone, Validate)]
pub struct LoginRequest {
    /// User to login as
    pub username: UserName,

    /// Pre-hashed password
    pub password: LoginPassword,

    /// Time-based One-Time Password token
    pub totp_token: Option<String>,

    /// How long the returned auth token should live
    pub lifetime: Option<Duration>,
}

#[derive(Serialize, Deserialize)]
pub enum LoginResponse {
    /// The login was successful and returned a session token
    Ok(ClientAuthToken),

    /// The request was invalid
    Invalid,

    /// The user account is expired
    Expired,

    /// The password and/or TOTP token were incorrect
    Denied,
}
