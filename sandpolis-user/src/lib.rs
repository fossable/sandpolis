#![feature(iterator_try_collect)]

// doc_comment! {
//     include_str!("../README.md")
// }

use anyhow::Result;
use anyhow::bail;
#[cfg(feature = "server")]
use jsonwebtoken::DecodingKey;
#[cfg(feature = "server")]
use jsonwebtoken::EncodingKey;
use native_db::ToKey;
use native_model::Model;
use sandpolis_core::RealmName;
use sandpolis_core::UserName;
use sandpolis_database::Resident;
use sandpolis_database::ResidentVec;
use sandpolis_database::{Data, DatabaseLayer};
use sandpolis_macros::data;
use sandpolis_network::ServerUrl;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::net::SocketAddr;
use validator::Validate;

#[cfg(feature = "client")]
pub mod client;
pub mod messages;
#[cfg(feature = "server")]
pub mod server;

#[data]
#[derive(Default)]
pub struct UserLayerData {}

#[derive(Clone)]
pub struct UserLayer {
    pub data: Resident<UserLayerData>,
    pub database: DatabaseLayer,
    #[cfg(feature = "server")]
    pub users: ResidentVec<UserData>,

    #[cfg(feature = "server")]
    pub jwt_keys: HashMap<RealmName, (EncodingKey, DecodingKey)>,
}

impl UserLayer {
    pub async fn new(database: DatabaseLayer) -> Result<Self> {
        Ok(Self {
            data: database.realm(RealmName::default())?.resident(())?,
            #[cfg(feature = "server")]
            users: database.realm(RealmName::default())?.resident_vec(())?,
            #[cfg(feature = "server")]
            jwt_keys: {
                let mut jwt_keys = HashMap::new();
                // TODO all realms
                let db = database.realm(RealmName::default())?;
                let rw = db.rw_transaction()?;
                let secrets: Vec<server::ServerJwtSecret> =
                    rw.scan().primary()?.all()?.try_collect()?;

                assert!(secrets.len() <= 1);
                let secret = if secrets.len() == 0 {
                    // Time to generate
                    let secret = server::ServerJwtSecret::default();
                    rw.insert(secret.clone())?;
                    rw.commit()?;

                    secret
                } else {
                    secrets[0]
                };

                jwt_keys.insert(
                    RealmName::default(),
                    (
                        EncodingKey::from_secret(&secret.value),
                        DecodingKey::from_secret(&secret.value),
                    ),
                );

                jwt_keys
            },
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
pub struct LoginAttemptData {
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

#[derive(Serialize, Deserialize, PartialEq, Clone, Debug)]
pub struct ClientAuthToken(pub String);
