use crate::user::UserName;
use crate::user::{ClientAuthToken, UserData};
use sandpolis_instance::ClusterId;
use serde::{Deserialize, Serialize};
use std::time::Duration;
use validator::Validate;

#[cfg(feature = "server")]
pub mod server;

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

    #[cfg(feature = "client")]
    pub fn strength(&self) {}
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
