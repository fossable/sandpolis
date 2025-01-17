use std::net::SocketAddr;

use anyhow::{bail, Result};
use serde::{Deserialize, Serialize};
use validator::Validate;

use crate::core::{InstanceId, InstanceType};

#[derive(Serialize, Deserialize, Validate, Debug)]
#[cfg_attr(feature = "client", derive(bevy::prelude::Component))]
pub struct UserData {
    /// Unchangable username
    #[validate(length(min = 4, max = 20))]
    pub username: String,

    /// Whether the user is an admin
    pub admin: bool,

    /// Email address
    #[validate(email)]
    pub email: Option<String>,

    /// Phone number
    pub phone: Option<String>,

    pub expiration: Option<i64>,
}

#[derive(Serialize, Deserialize)]
#[cfg_attr(feature = "client", derive(bevy::prelude::Component))]
pub struct LoginAttempt {
    pub timestamp: u64,

    pub address: SocketAddr,
}

/// Create a new user account.
#[derive(Serialize, Deserialize, Validate)]
pub struct CreateUserRequest {
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
    pub username: Option<String>,

    /// Search by email prefix
    pub email: Option<String>,
}

#[derive(Serialize, Deserialize)]
pub enum GetUsersResponse {
    Ok(Vec<UserData>),
    PermissionDenied,
}

/// Update an existing user account.
#[derive(Serialize, Deserialize)]
pub struct UpdateUserRequest {
    /// User to edit
    pub username: String,

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

/// Prehashed password to avoid sending a plaintext password to the server. The
/// password is salted with a static value to prevent hash shucking.
pub struct PrehashedPassword(String);

impl PrehashedPassword {
    pub fn new(server_id: InstanceId, plaintext: &str) -> Result<Self> {
        if !server_id.check(InstanceType::Server) {
            bail!("Server ID required");
        }
        todo!()
    }
}

/// Request a login from the server
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct LoginRequest {
    /// User to login as
    pub username: String,

    /// Password as unsalted hash
    pub password: String,

    /// Time-based One-Time Password token
    pub totp_token: Option<String>,
}

#[derive(Serialize, Deserialize)]
pub enum LoginResponse {
    /// The login was successful and returned a session token
    Ok(String),

    /// The request was invalid
    Invalid,

    /// The user account is expired
    Expired,

    /// The password and/or TOTP token were incorrect
    Denied,
}
