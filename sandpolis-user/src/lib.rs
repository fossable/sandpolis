//! ## User sessions
//!
//! Once a user is logged in successfully, a session token (JWT) is returned
//! with a lifetime of 20 minutes. If a client determines the user is not idle,
//! the token can be automatically renewed.

use std::{net::SocketAddr, ops::Deref, str::FromStr};

use anyhow::{Result, bail};
use regex::Regex;
use sandpolis_database::{Collection, Database, Document};
use serde::{Deserialize, Serialize};
use validator::{Validate, ValidationErrors};

#[cfg(feature = "client")]
pub mod client;

#[cfg(feature = "server")]
pub mod server;

pub mod messages;

#[derive(Serialize, Deserialize, Debug, Clone, Default)]
pub struct UserLayerData {}

#[derive(Clone)]
pub struct UserLayer {
    pub data: DataView<UserLayerData>,
    #[cfg(feature = "server")]
    pub users: DataView<UserData>,
}

impl UserLayer {
    pub fn new(data: Document<UserLayerData>) -> Result<Self> {
        Ok(Self {
            #[cfg(feature = "server")]
            users: data.collection("/users")?,
            data,
        })
    }
}

#[derive(Serialize, Deserialize, PartialEq, Debug, Validate)]
#[native_model(id = 12, version = 1)]
#[native_db]
pub struct UserData {
    #[primary_key]
    pub _id: u32,

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

#[derive(Serialize, Deserialize, PartialEq, Debug)]
#[native_model(id = 13, version = 1)]
#[native_db]
pub struct LoginAttempt {
    #[primary_key]
    pub _id: u32,

    /// When the login attempt occurred
    pub timestamp: u64,

    pub username: UserName,

    /// Source address of the login attempt
    pub source: SocketAddr,

    /// Whether the login attempt was successful
    pub allowed: bool,
}

/// This password is "pre-hashed" and salted with the cluster ID to avoid _hash
/// shucking_ attacks. The server will hash and salt this value with a random
/// value.
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct LoginPassword(pub String);

pub enum UserPermission {
    Create,
    List,
    Delete,
}
