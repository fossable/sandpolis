use anyhow::{Result, bail};
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
use native_db::ToKey;
use native_model::Model;
use passwords::PasswordGenerator;
use rand::Rng;
use ring::pbkdf2;
use sandpolis_core::{RealmName, UserName};
use sandpolis_database::DataRevision;
use sandpolis_macros::data;
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

static KEY: LazyLock<ServerKey> = LazyLock::new(|| ServerKey::new());

static USER_PASSWORD_HASH_ITERATIONS: NonZeroU32 = NonZeroU32::new(15000).unwrap();

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

#[derive(Validate)]
#[data(temporal)]
pub struct PasswordData {
    /// User that this password belongs to
    #[secondary_key]
    pub user: UserName,

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

impl UserLayer {
    // TODO better users.find
    pub async fn user(&self, username: &UserName) -> Result<UserData> {
        let user = for user in self.users.iter() {
            if user.read().username == *username {
                return Ok(user.read().clone());
            }
        };

        bail!("User not found");
    }

    /// Create an admin user if one doesn't exist already. The password will be
    /// emitted in the server log if created.
    pub async fn try_create_admin(&self) -> Result<()> {
        for user in self.users.iter() {
            if user.read().admin {
                return Ok(());
            }
        }

        self.users.push(UserData {
            username: "admin".parse()?,
            admin: true,
            email: None,
            phone: None,
            expiration: None,
            ..Default::default()
        })?;

        // Generate a default password
        let password = PasswordGenerator::new()
            .length(8)
            .numbers(true)
            .lowercase_letters(true)
            .uppercase_letters(true)
            .symbols(true)
            .spaces(true)
            .exclude_similar_characters(true)
            .strict(false)
            .generate_one()
            .unwrap();

        self.new_password("admin".parse()?, &password).await?;
        info!(username = "admin", password = %password, "Created default admin user");
        Ok(())
    }

    /// Create a new password without a TOTP.
    pub async fn new_password(&self, user: UserName, password: &str) -> Result<PasswordData> {
        // Precondition: user exists
        // TODO

        // Precondition: no password exists for this user yet
        // TODO

        let salt = rand::rng().random::<[u8; 32]>().to_vec();
        let mut hash = Vec::new();

        pbkdf2::derive(
            pbkdf2::PBKDF2_HMAC_SHA256,
            USER_PASSWORD_HASH_ITERATIONS,
            &salt,
            password.as_bytes(),
            &mut hash,
        );

        let db = self.database.realm(RealmName::default())?;
        let rw = db.rw_transaction()?;

        let password = PasswordData {
            user,
            iterations: USER_PASSWORD_HASH_ITERATIONS.get(),
            salt,
            hash,
            totp_secret: None,
            ..Default::default()
        };
        rw.insert(password.clone())?;
        rw.commit()?;

        Ok(password)
    }

    /// Create a new password with a TOTP.
    pub async fn new_password_with_totp(
        &self,
        user: UserName,
        password: &str,
    ) -> Result<PasswordData> {
        // Precondition: user exists
        // TODO

        // Precondition: no password exists for this user yet
        // TODO

        let salt = rand::rng().random::<[u8; 32]>().to_vec();
        let mut hash = Vec::new();

        pbkdf2::derive(
            pbkdf2::PBKDF2_HMAC_SHA256,
            USER_PASSWORD_HASH_ITERATIONS,
            &salt,
            password.as_bytes(),
            &mut hash,
        );

        let db = self.database.realm(RealmName::default())?;
        let rw = db.rw_transaction()?;

        let password = PasswordData {
            iterations: USER_PASSWORD_HASH_ITERATIONS.get(),
            salt,
            hash,
            totp_secret: Some(
                TOTP::new(
                    totp_rs::Algorithm::SHA1,
                    6,
                    1,
                    30,
                    Secret::default().to_bytes()?,
                    Some("Sandpolis".to_string()),
                    user.to_string(),
                )?
                .get_url(),
            ),
            user,
            ..Default::default()
        };
        rw.insert(password.clone())?;
        rw.commit()?;

        Ok(password)
    }

    pub async fn password(&self, user: UserName) -> Result<PasswordData> {
        let db = self.database.realm(RealmName::default())?;
        let r = db.r_transaction()?;

        let passwords: Vec<PasswordData> = r
            .scan()
            .secondary(PasswordDataKey::user)?
            .equal(user)?
            .and(
                r.scan()
                    .secondary(PasswordDataKey::_revision)?
                    .equal(DataRevision::Latest(0))?,
            )
            .try_collect()?;

        if passwords.len() != 1 {
            bail!("Failed to get password");
        }

        Ok(passwords[0].to_owned())
    }
}

impl Display for PasswordData {
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
