use anyhow::Result;
use axum::{
    RequestPartsExt, Router,
    extract::FromRequestParts,
    http::{StatusCode, request::Parts},
    routing::{get, post},
};
use axum_extra::{
    TypedHeader,
    headers::{Authorization, authorization::Bearer},
};
use jsonwebtoken::{DecodingKey, EncodingKey, Validation, decode};
use rand::Rng;
use ring::pbkdf2;
use serde::{Deserialize, Serialize};
use std::sync::LazyLock;
use std::{
    fmt::{Debug, Display},
    num::NonZeroU32,
};
use totp_rs::{Secret, TOTP};
use tracing::info;
use validator::Validate;

use super::UserData;
use super::UserLayer;

pub mod routes;

impl UserLayer {
    /// Create an admin user if one doesn't exist already. The password will be
    /// emitted in the server log.
    pub fn create_admin(&self) -> Result<()> {
        if self
            .users
            .documents()
            .filter_map(|user| user.ok())
            .find(|user| user.data.admin)
            .is_none()
        {
            let user = self.users.insert_document(
                "admin",
                UserData {
                    username: "admin".parse()?,
                    admin: true,
                    email: None,
                    phone: None,
                    expiration: None,
                },
            )?;

            let default = "test"; // TODO hash
            // TODO transaction
            user.insert_document("password", PasswordData::new(&default))?;
            info!(username = "admin", password = %default, "Created default admin user");
        }
        Ok(())
    }
}

static KEY: LazyLock<ServerKey> = LazyLock::new(|| ServerKey::new());

struct ServerKey {
    encoding: EncodingKey,
    decoding: DecodingKey,
}

impl ServerKey {
    fn new() -> Self {
        let secret = rand::rng().random::<[u8; 32]>().to_vec();
        Self {
            encoding: EncodingKey::from_secret(&secret),
            decoding: DecodingKey::from_secret(&secret),
        }
    }
}

#[derive(Serialize, Deserialize, Validate, Clone)]
pub struct PasswordData {
    /// Number of rounds to use when hashing password
    #[validate(range(min = 4284, max = 200000))]
    pub iterations: u32,

    /// Random data used to salt the password hash
    pub salt: Vec<u8>,

    /// Password hash
    pub hash: Vec<u8>,

    /// TOTP secret token
    pub totp_secret: Option<String>,
}

impl PasswordData {
    /// Create a new password without a TOTP.
    pub fn new(password: &str) -> Self {
        let mut data = PasswordData {
            iterations: 15000,
            salt: rand::rng().random::<[u8; 32]>().to_vec(),
            hash: vec![0u8; ring::digest::SHA256_OUTPUT_LEN],
            totp_secret: None,
        };
        pbkdf2::derive(
            pbkdf2::PBKDF2_HMAC_SHA256,
            NonZeroU32::new(data.iterations).expect("nonzero"),
            &data.salt,
            password.as_bytes(),
            &mut data.hash,
        );
        data
    }

    /// Create a new password with a TOTP.
    pub fn new_with_totp(username: &str, password: &str) -> Result<Self> {
        let mut data = PasswordData {
            iterations: 15000,
            salt: rand::rng().random::<[u8; 32]>().to_vec(),
            hash: Vec::new(),
            totp_secret: Some(
                TOTP::new(
                    totp_rs::Algorithm::SHA1,
                    6,
                    1,
                    30,
                    Secret::default().to_bytes()?,
                    Some("Sandpolis".to_string()),
                    username.to_string(),
                )?
                .get_url(),
            ),
        };
        pbkdf2::derive(
            pbkdf2::PBKDF2_HMAC_SHA256,
            NonZeroU32::new(data.iterations).expect("nonzero"),
            &data.salt,
            password.as_bytes(),
            &mut data.hash,
        );
        Ok(data)
    }
}

impl Display for PasswordData {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        todo!()
    }
}

impl Debug for PasswordData {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("PasswordData")
            .field("iterations", &self.iterations)
            .field("salt", &self.salt)
            .finish()
    }
}

#[derive(Debug, Serialize, Deserialize)]
pub struct Claims {
    /// Username
    sub: String,

    /// Claim expiration
    exp: usize,

    /// Whether the user is an admin
    admin: bool,
}

impl<S> FromRequestParts<S> for Claims
where
    S: Send + Sync,
{
    type Rejection = StatusCode;

    async fn from_request_parts(parts: &mut Parts, _state: &S) -> Result<Self, Self::Rejection> {
        // Extract the token from the authorization header
        let TypedHeader(Authorization(bearer)) = parts
            .extract::<TypedHeader<Authorization<Bearer>>>()
            .await
            .map_err(|_| StatusCode::BAD_REQUEST)?;

        let token_data = decode::<Claims>(bearer.token(), &KEY.decoding, &Validation::default())
            .map_err(|_| StatusCode::FORBIDDEN)?;

        Ok(token_data.claims)
    }
}
