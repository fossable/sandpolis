use crate::DbConnection;
use anyhow::Result;
use core_protocol::core::protocol::GetUserResponse;
use ring::pbkdf2;
use serde::{Deserialize, Serialize};
use std::num::NonZeroU32;
use validator::Validate;

#[derive(Clone, Serialize, Deserialize, Default)]
pub struct PasswordHash {
    pub iterations: u32,

    pub salt: String,

    pub hash: String,
}

#[derive(Clone, Serialize, Deserialize, Validate, Default)]
pub struct User {
    /// The user's unchangable username
    #[validate(length(min = 4), length(max = 20))]
    pub username: String,

    /// Whether the user is an admin
    pub admin: bool,

    /// The user's password hash
    pub password: PasswordHash,

    /// The user's TOTP secret token
    pub totp_secret: Option<String>,

    /// The user's optional email address
    #[validate(email)]
    pub email: Option<String>,

    /// The user's optional phone number
    #[validate(phone)]
    pub phone: Option<String>,

    pub expiration: Option<i64>,

    #[serde(skip)]
    pub sessions: Vec<String>,
}

impl User {
    pub fn verify_login(&self, password: &str, totp_token: i32) -> bool {
        if !pbkdf2::verify(
            pbkdf2::PBKDF2_HMAC_SHA256,
            NonZeroU32::new(self.password.iterations).unwrap_or(NonZeroU32::new(1).unwrap()),
            self.password.salt.as_bytes(),
            password.as_bytes(),
            self.password.hash.as_bytes(),
        )
        .is_ok()
        {
            return false;
        }

        return true;
    }
}

impl From<User> for GetUserResponse {
    fn from(user: User) -> GetUserResponse {
        GetUserResponse {
            username: user.username,
            phone: user.phone.unwrap_or(String::new()),
            email: user.email.unwrap_or(String::new()),
            expiration: user.expiration.unwrap_or(0i64),
        }
    }
}
