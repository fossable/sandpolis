// doc_comment! {
//     include_str!("../README.md")
// }

use anyhow::Result;
use native_db::ToKey;
use native_model::Model;
use sandpolis_core::UserName;
use sandpolis_database::ResidentVec;
use sandpolis_database::{Data, DatabaseLayer};
use sandpolis_macros::data;
use serde::{Deserialize, Serialize};
use std::net::SocketAddr;
use validator::Validate;

#[cfg(feature = "client")]
pub mod client;

#[cfg(feature = "server")]
pub mod server;

pub mod messages;

#[data]
pub struct UserLayerData {}

#[derive(Clone)]
pub struct UserLayer {
    pub database: DatabaseLayer,
    #[cfg(feature = "server")]
    pub users: ResidentVec<UserData>,
}

impl UserLayer {
    pub async fn new(database: DatabaseLayer) -> Result<Self> {
        Ok(Self {
            #[cfg(feature = "server")]
            users: database
                .realm(RealmName::default())
                .await?
                .resident_vec(())?,
            database,
        })
    }
}

#[derive(Validate)]
#[data]
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

#[data]
pub struct LoginAttempt {
    /// When the login attempt occurred
    pub timestamp: u64,

    pub username: UserName,

    /// Source address of the login attempt
    pub source: Option<SocketAddr>,

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
