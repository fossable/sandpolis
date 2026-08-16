use crate::user::ClientAuthToken;
use crate::user::UserName;
#[cfg(any(feature = "client", feature = "server"))]
use argon2::{
    Argon2,
    password_hash::{PasswordHasher, SaltString},
};
#[cfg(any(feature = "client", feature = "server"))]
use base64::prelude::*;
use native_db::ToKey;
use native_model::Model;
use sandpolis_instance::ClusterId;
use sandpolis_macros::data;
use serde::{Deserialize, Serialize};
use std::net::SocketAddr;
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

    /// True when the client is knowingly setting the user's initial password:
    /// it already saw [`LoginResponse::PasswordSetupRequired`] and had the user
    /// confirm the password.
    #[serde(default)]
    pub setup: bool,

    /// Time-based One-Time Password token
    pub totp_token: Option<String>,

    /// How long the returned auth token should live
    pub lifetime: Option<Duration>,
}

#[derive(Serialize, Deserialize, Debug)]
pub enum LoginResponse {
    /// The login was successful and returned a session token
    Ok(ClientAuthToken),

    /// The user exists but has no password yet. The client shows a set-password
    /// dialog (with confirmation) and retries with `setup: true`.
    PasswordSetupRequired {
        /// Whether the realm requires TOTP enrollment, so the client can warn
        /// the user before the enrollment response arrives.
        totp_required: bool,
    },

    /// The password was stored and a TOTP secret was generated. The client
    /// displays the otpauth URL for the user to enroll, then retries the login
    /// with a code.
    TotpSetupRequired { otpauth_url: String },

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
#[derive(Default)]
pub struct LoginAttemptData {
    /// When the login attempt occurred
    pub timestamp: u64,

    pub username: UserName,

    /// Source address of the login attempt
    pub source: Option<SocketAddr>,

    /// Whether the login attempt was successful
    pub allowed: bool,
}
