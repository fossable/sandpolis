//! ## User sessions
//!
//! Once a user is logged in successfully, a session token (JWT) is returned with
//! a lifetime of 20 minutes. If a client determines the user is not idle, the
//! token can be automatically renewed.

use std::{net::SocketAddr, ops::Deref, str::FromStr};

use anyhow::{bail, Result};
use regex::Regex;
use sandpolis_database::{Collection, Database};
use serde::{Deserialize, Serialize};
use validator::{Validate, ValidationErrors};

#[cfg(feature = "client")]
pub mod client;

#[cfg(feature = "server")]
pub mod server;

pub mod messages;

#[derive(Clone)]
pub struct UserLayer {
    #[cfg(any(feature = "server", feature = "client"))]
    pub users: Collection<UserData>,
}

impl UserLayer {
    pub fn new(db: Database) -> Result<Self> {
        Ok(Self {
            #[cfg(any(feature = "server", feature = "client"))]
            users: db.collection("/server/users")?,
        })
    }
}

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

    /// Source address of the login attempt
    pub source: SocketAddr,

    /// Whether the login attempt was successful
    pub allowed: bool,
}

/// This password is "pre-hashed" and salted with the cluster ID to avoid _hash shucking_
/// attacks. The server will hash and salt this value with a random value.
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct LoginPassword(pub String);
