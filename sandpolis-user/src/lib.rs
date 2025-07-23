#![feature(iterator_try_collect)]

// doc_comment! {
//     include_str!("../README.md")
// }

use anyhow::bail;
use anyhow::{Result, anyhow};
use argon2::{
    Argon2,
    password_hash::{PasswordHash, PasswordHasher, PasswordVerifier, SaltString, rand_core::OsRng},
};
use base64::prelude::*;
use native_db::ToKey;
use native_model::Model;
use sandpolis_core::ClusterId;
use sandpolis_core::RealmName;
use sandpolis_core::UserName;
use sandpolis_database::Resident;
use sandpolis_database::ResidentVec;
use sandpolis_database::{Data, DatabaseLayer};
use sandpolis_instance::InstanceLayer;
use sandpolis_macros::data;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::net::SocketAddr;
use tracing::debug;
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
    pub instance: InstanceLayer,
    pub database: DatabaseLayer,
    #[cfg(feature = "server")]
    pub users: ResidentVec<UserData>,

    #[cfg(feature = "server")]
    pub jwt_keys: HashMap<RealmName, (jsonwebtoken::EncodingKey, jsonwebtoken::DecodingKey)>,
}

impl UserLayer {
    pub async fn new(instance: InstanceLayer, database: DatabaseLayer) -> Result<Self> {
        debug!("Initializing user layer");
        let user_layer = Self {
            instance,
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
                    debug!("Generating new JWT secrets");

                    let secret = server::ServerJwtSecret::new();
                    rw.insert(secret.clone())?;
                    rw.commit()?;

                    secret
                } else {
                    secrets[0].clone()
                };

                jwt_keys.insert(
                    RealmName::default(),
                    (
                        jsonwebtoken::EncodingKey::from_secret(&secret.value),
                        jsonwebtoken::DecodingKey::from_secret(&secret.value),
                    ),
                );

                jwt_keys
            },
            database,
        };

        #[cfg(feature = "server")]
        user_layer.try_create_admin().await?;

        Ok(user_layer)
    }
}

#[data]
#[derive(Validate, Default)]
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

impl LoginPassword {
    /// When creating a `LoginPassword`, the cluster id is used as the initial
    /// salt to ensure the same password in different clusters has different
    /// initial hashes.
    #[cfg(any(feature = "client", feature = "server"))]
    pub fn new(cluster_id: ClusterId, plaintext: &str) -> Self {
        let h = Argon2::default()
            .hash_password(
                plaintext.as_bytes(),
                &SaltString::from_b64(&BASE64_STANDARD_NO_PAD.encode(cluster_id.as_bytes()))
                    .expect("Cluster ID is always base64-able"),
            )
            .expect("Salt is base64")
            .to_string();

        Self(h)
    }
}

pub enum UserPermission {
    Create,
    List,
    Delete,
}

#[derive(Serialize, Deserialize, PartialEq, Clone, Debug)]
pub struct ClientAuthToken(pub String);
