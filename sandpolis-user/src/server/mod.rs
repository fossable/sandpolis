use anyhow::{Result, anyhow, bail};
use aws_lc_rs::pbkdf2;
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
use sandpolis_core::{RealmName, UserName};

const SHA256_OUTPUT_LEN: usize = 32;
use axum::extract::{self, WebSocketUpgrade};
use axum::extract::{Request, State};
use axum::middleware::Next;
use sandpolis_database::DataRevision;
use sandpolis_macros::data;
use sandpolis_network::{InstanceConnection, RequestResult};
use serde::{Deserialize, Serialize};
use std::{
    fmt::{Debug, Display},
    num::NonZeroU32,
};
use totp_rs::{Secret, TOTP};
use tracing::info;
use validator::Validate;

use crate::{ClientAuthToken, LoginPassword};

use super::UserData;
use super::UserLayer;

pub mod routes;

static USER_PASSWORD_HASH_ITERATIONS: NonZeroU32 = NonZeroU32::new(15000).unwrap();

#[data]
#[derive(Default)]
pub struct ServerJwtSecret {
    #[serde(with = "serde_bytes")]
    pub value: [u8; 32],
}

impl ServerJwtSecret {
    pub fn new() -> Self {
        Self {
            value: rand::rng().random::<[u8; 32]>(),
            ..Default::default()
        }
    }
}

#[data(temporal)]
#[derive(Validate, Default)]
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
            .symbols(false)
            .spaces(false)
            .exclude_similar_characters(true)
            .strict(false)
            .generate_one()
            .unwrap();

        self.new_password(
            "admin".parse()?,
            LoginPassword::new(self.instance.cluster_id, &password),
        )
        .await?;
        info!(username = "admin", password = %password, "Created default admin user");
        Ok(())
    }

    /// Create a new password without a TOTP.
    pub async fn new_password(
        &self,
        user: UserName,
        password: LoginPassword,
    ) -> Result<PasswordData> {
        // Precondition: user exists
        // TODO

        // Precondition: no password exists for this user yet
        // TODO

        let salt = rand::rng().random::<[u8; 32]>().to_vec();
        let mut hash = [0u8; SHA256_OUTPUT_LEN];

        pbkdf2::derive(
            pbkdf2::PBKDF2_HMAC_SHA256,
            USER_PASSWORD_HASH_ITERATIONS,
            &salt,
            password.0.as_bytes(),
            &mut hash,
        );

        let db = self.database.realm(RealmName::default())?;
        let rw = db.rw_transaction()?;

        let password = PasswordData {
            user,
            iterations: USER_PASSWORD_HASH_ITERATIONS.get(),
            salt,
            hash: hash.to_vec(),
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
        password: LoginPassword,
    ) -> Result<PasswordData> {
        // Precondition: user exists
        // TODO

        // Precondition: no password exists for this user yet
        // TODO

        let salt = rand::rng().random::<[u8; 32]>().to_vec();
        let mut hash = [0u8; SHA256_OUTPUT_LEN];

        pbkdf2::derive(
            pbkdf2::PBKDF2_HMAC_SHA256,
            USER_PASSWORD_HASH_ITERATIONS,
            &salt,
            password.0.as_bytes(),
            &mut hash,
        );

        let db = self.database.realm(RealmName::default())?;
        let rw = db.rw_transaction()?;

        let password = PasswordData {
            iterations: USER_PASSWORD_HASH_ITERATIONS.get(),
            salt,
            hash: hash.to_vec(),
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
            // .and(
            //     r.scan()
            //         .secondary(PasswordDataKey::_revision)?
            //         .equal(DataRevision::Latest(0))?,
            // )
            .collect::<Result<Vec<_>, _>>()?;

        if passwords.len() == 0 {
            bail!("Password not found");
        } else if passwords.len() > 1 {
            bail!("Too many passwords found");
        }

        Ok(passwords[0].to_owned())
    }

    pub fn new_token(&self, claims: Claims) -> Result<ClientAuthToken> {
        Ok(ClientAuthToken(jsonwebtoken::encode(
            &jsonwebtoken::Header::default(),
            &claims,
            &self
                .jwt_keys
                .get(&claims.realm)
                .ok_or(anyhow!("Realm not found"))?
                .0,
        )?))
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
    pub sub: UserName,

    /// Claim expiration
    pub exp: usize,

    /// Whether the user is an admin
    pub admin: bool,

    /// Realm in which these claims exist
    pub realm: RealmName,
}

impl FromRequestParts<UserLayer> for Claims {
    type Rejection = StatusCode;

    async fn from_request_parts(
        parts: &mut Parts,
        state: &UserLayer,
    ) -> Result<Self, Self::Rejection> {
        // Extract the token from the authorization header
        let TypedHeader(Authorization(bearer)) = parts
            .extract::<TypedHeader<Authorization<Bearer>>>()
            .await
            .map_err(|_| StatusCode::BAD_REQUEST)?;

        // TODO if we can get this from parts.extentions provided by `auth_middleware`,
        // then we won't need to send the header at all.
        let TypedHeader(realm) = parts
            .extract::<TypedHeader<RealmName>>()
            .await
            .map_err(|_| StatusCode::BAD_REQUEST)?;

        let token_data = decode::<Claims>(
            bearer.token(),
            &state.jwt_keys.get(&realm).ok_or(StatusCode::BAD_REQUEST)?.1,
            &Validation::default(),
        )
        .map_err(|_| StatusCode::FORBIDDEN)?;

        Ok(token_data.claims)
    }
}

#[axum_macros::debug_handler]
pub async fn connect(
    State(state): State<UserLayer>,
    claims: Claims,
    ws: WebSocketUpgrade,
) -> impl axum::response::IntoResponse {
    let database = state.database.clone();
    let realm = claims.realm.clone();
    let cluster_id = state.instance.cluster_id;
    ws.on_upgrade(move |socket| async move {
        let data = database.realm(realm.clone()).unwrap().resident(()).unwrap();
        // Collect all registered responder handlers from inventory
        let handlers: Vec<&dyn sandpolis_network::RegisterResponders> =
            sandpolis_network::collected_responders().collect();
        InstanceConnection::websocket(socket, data, realm, cluster_id, &handlers);
    })
}
