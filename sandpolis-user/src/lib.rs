//! ## User sessions
//!
//! Once a user is logged in successfully, a session token (JWT) is returned with
//! a lifetime of 20 minutes. If a client determines the user is not idle, the
//! token can be automatically renewed.

use std::{net::SocketAddr, ops::Deref, str::FromStr};

use anyhow::{bail, Result};
use regex::Regex;
use sandpolis_instance::{ClusterId, InstanceId, InstanceType};
use serde::{Deserialize, Serialize};
use validator::{Validate, ValidationErrors};

#[cfg(feature = "client")]
pub mod client;

#[cfg(feature = "server")]
pub mod server;

#[derive(Serialize, Deserialize, Validate, Debug, Clone)]
pub struct UserData {
    pub username: UserName,

    /// Whether the user is an admin
    pub admin: bool,

    /// Email address
    #[validate(email)]
    pub email: Option<String>,

    /// Phone number
    pub phone: Option<String>,

    pub expiration: Option<i64>,
}

/// A user's username is forever unchangable.
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct UserName(String);

impl Deref for UserName {
    type Target = String;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl FromStr for UserName {
    type Err = anyhow::Error;

    fn from_str(s: &str) -> Result<Self> {
        let name = UserName(s.to_string());
        name.validate()?;
        Ok(name)
    }
}

impl Validate for UserName {
    fn validate(&self) -> Result<(), ValidationErrors> {
        if Regex::new("^[a-z0-9]{4,32}$").unwrap().is_match(&self.0) {
            Ok(())
        } else {
            Err(ValidationErrors::new())
        }
    }
}

#[derive(Serialize, Deserialize)]
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
#[derive(Serialize, Deserialize)]
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

/// This password is "pre-hashed" and salted with the cluster ID to avoid _hash shucking_
/// attacks. The server will hash and salt this value with a random value.
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct LoginPassword(pub String);

/// Request a login from the server
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct LoginRequest {
    /// User to login as
    pub username: String,

    /// Pre-hashed password
    pub password: LoginPassword,

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
