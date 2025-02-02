use super::LoginPassword;
use anyhow::{anyhow, Result};
use argon2::{
    password_hash::{rand_core::OsRng, PasswordHash, PasswordHasher, PasswordVerifier, SaltString},
    Argon2,
};
use base64::prelude::*;
use sandpolis_instance::ClusterId;

impl LoginPassword {
    pub fn new(cluster_id: ClusterId, plaintext: &str) -> Result<Self> {
        let salt = SaltString::from_b64(&BASE64_STANDARD_NO_PAD.encode(cluster_id.as_bytes()))
            .map_err(|err| anyhow!(""))?;

        Ok(Self(
            Argon2::default()
                .hash_password(plaintext.as_bytes(), &salt)
                .map_err(|err| anyhow!(""))?
                .to_string(),
        ))
    }
}
