use super::LoginPassword;
use anyhow::{Result, anyhow};
use argon2::{
    Argon2,
    password_hash::{PasswordHash, PasswordHasher, PasswordVerifier, SaltString, rand_core::OsRng},
};
use base64::prelude::*;
use sandpolis_instance::ClusterId;

impl LoginPassword {
    /// When creating a `LoginPassword`, the cluster id is used as the initial
    /// salt to ensure the same password in different clusters has different
    /// initial hashes.
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
