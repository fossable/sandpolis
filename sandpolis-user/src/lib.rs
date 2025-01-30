//! ## User sessions
//!
//! Once a user is logged in successfully, a session token (JWT) is returned with
//! a lifetime of 20 minutes. If a client determines the user is not idle, the
//! token can be automatically renewed.

use std::{net::SocketAddr, ops::Deref, str::FromStr};

use anyhow::{bail, Result};
use regex::Regex;
use serde::{Deserialize, Serialize};
use validator::{Validate, ValidationErrors};

use crate::core::{ClusterId, InstanceId, InstanceType};

#[derive(Serialize, Deserialize, Validate, Debug)]
#[cfg_attr(feature = "client", derive(bevy::prelude::Component))]
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
#[derive(Serialize, Deserialize, Debug)]
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

/// To avoid sending plaintext passwords around, this password is hashed and
/// salted with the cluster ID. The server will also hash and salt this value.
#[cfg(any(feature = "server", feature = "client"))]
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct LoginPassword(pub String);

impl LoginPassword {
    pub fn new(cluster_id: ClusterId, plaintext: &str) -> Result<Self> {
        use argon2::{
            password_hash::{
                rand_core::OsRng, PasswordHash, PasswordHasher, PasswordVerifier, SaltString,
            },
            Argon2,
        };

        // Ok(Self(
        //     Argon2::default()
        //         .hash_password(plaintext.as_bytes(), cluster_id.try_into()?)
        //         .map_err(|err| todo!())?
        //         .to_string(),
        // ))
        todo!()
    }
}

/// Request a login from the server
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct LoginRequest {
    /// User to login as
    pub username: String,

    /// Password as unsalted hash
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
