use sandpolis_instance::ClusterId;
use super::LoginPassword;
use anyhow::Result;


impl LoginPassword {
    pub fn new(cluster_id: ClusterId, plaintext: &str) -> Result<Self> {
        use argon2::{
            password_hash::{
                rand_core::OsRng, PasswordHash, PasswordHasher, PasswordVerifier, SaltString,
            },
            Argon2,
        };

        // Ok(Self(
        //     Argon2::default()
        //         .hash_password(plaintext.as_bytes(), cluster_id.try_into()?)
        //         .map_err(|err| todo!())?
        //         .to_string(),
        // ))
        todo!()
    }
}
