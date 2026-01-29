use crate::login::LoginPassword;
use anyhow::Result;
#[cfg(any(feature = "client", feature = "server"))]
use argon2::{
    Argon2,
    password_hash::{PasswordHasher, SaltString},
};
use base64::prelude::*;
use native_db::ToKey;
use native_model::Model;
use regex::Regex;
use sandpolis_instance::ClusterId;
use sandpolis_instance::InstanceLayer;
use sandpolis_instance::database::ResidentVec;
use sandpolis_instance::database::{DatabaseLayer, Resident};
use sandpolis_instance::realm::RealmName;
use sandpolis_macros::data;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::fmt::Display;
use std::net::SocketAddr;
use std::ops::Deref;
use std::str::FromStr;
use std::sync::LazyLock;
use std::time::Duration;
use tracing::debug;
use validator::{Validate, ValidationErrors};

#[cfg(feature = "server")]
pub mod server;

static USER_NAME_REGEX: LazyLock<Regex> = LazyLock::new(|| Regex::new("^[a-z0-9]{4,32}$").unwrap());

/// Create a new user account.
#[derive(Serialize, Deserialize, Validate)]
pub struct CreateUserRequest {
    // TODO inline
    pub data: UserData,

    /// Password as unsalted hash
    pub password: LoginPassword,

    /// Whether a TOTP secret should be generated
    pub totp: bool,
}

#[derive(Serialize, Deserialize)]
pub enum CreateUserResponse {
    Ok {
        /// TOTP secret URL
        totp_secret: Option<String>,
    },
    Failed,
    InvalidUser,
}

#[derive(Serialize, Deserialize)]
pub struct GetUsersRequest {
    /// Search by username prefix
    pub username: Option<UserName>,

    /// Search by email prefix
    pub email: Option<String>,
}

#[derive(Serialize, Deserialize)]
pub enum GetUsersResponse {
    Ok(Vec<UserData>),
    PermissionDenied,
}

/// Update an existing user account.
#[derive(Serialize, Deserialize, Validate)]
pub struct UpdateUserRequest {
    /// User to edit
    pub username: UserName,

    /// New password
    pub password: Option<String>,

    /// New email
    pub email: Option<String>,

    /// New phone number
    pub phone: Option<String>,

    /// New expiration timestamp
    pub expiration: Option<u64>,
}

#[derive(Serialize, Deserialize)]
pub enum UpdateUserResponse {
    Ok,

    /// The requested user does not exist
    NotFound,
}

/// A user's username is forever unchangable.
#[derive(Serialize, Deserialize, Debug, Clone, PartialEq, Eq)]
pub struct UserName(String);

impl Deref for UserName {
    type Target = String;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl FromStr for UserName {
    type Err = anyhow::Error;

    fn from_str(s: &str) -> Result<Self> {
        let name = UserName(s.to_string());
        name.validate()?;
        Ok(name)
    }
}

impl Validate for UserName {
    fn validate(&self) -> Result<(), ValidationErrors> {
        if USER_NAME_REGEX.is_match(&self.0) {
            Ok(())
        } else {
            Err(ValidationErrors::new())
        }
    }
}

impl Default for UserName {
    fn default() -> Self {
        UserName("admin".to_string())
    }
}

impl Display for UserName {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(&self.0)
    }
}

impl ToKey for UserName {
    fn to_key(&self) -> native_db::Key {
        native_db::Key::new(self.0.as_bytes().to_vec())
    }

    fn key_names() -> Vec<String> {
        vec!["UserName".to_string()]
    }
}

#[cfg(test)]
mod test_user_name {
    use super::*;

    #[test]
    fn test_valid_usernames() {
        assert!("test".parse::<UserName>().is_ok());
        assert!("admin".parse::<UserName>().is_ok());
        assert!("user123".parse::<UserName>().is_ok());
        assert!("1234".parse::<UserName>().is_ok());
        assert!("abcd".parse::<UserName>().is_ok());
        assert!("user0".parse::<UserName>().is_ok());
        assert!("0user".parse::<UserName>().is_ok());
        assert!(
            "longusername1234567890123456789012"
                .parse::<UserName>()
                .is_ok()
        );
    }

    #[test]
    fn test_invalid_usernames() {
        // Too short
        assert!("a".parse::<UserName>().is_err());
        assert!("ab".parse::<UserName>().is_err());
        assert!("abc".parse::<UserName>().is_err());
        assert!("".parse::<UserName>().is_err());

        // Too long
        assert!(
            "verylongusernamethatexceedsthemaximumlengthallowed"
                .parse::<UserName>()
                .is_err()
        );
        assert!("a".repeat(33).parse::<UserName>().is_err());

        // Invalid characters
        assert!("user-name".parse::<UserName>().is_err());
        assert!("user_name".parse::<UserName>().is_err());
        assert!("user.name".parse::<UserName>().is_err());
        assert!("user@name".parse::<UserName>().is_err());
        assert!("user name".parse::<UserName>().is_err());
        assert!("User".parse::<UserName>().is_err());
        assert!("USER".parse::<UserName>().is_err());
        assert!("user!".parse::<UserName>().is_err());
        assert!("user#".parse::<UserName>().is_err());
        assert!("user$".parse::<UserName>().is_err());
        assert!("user%".parse::<UserName>().is_err());
        assert!("user^".parse::<UserName>().is_err());
        assert!("user&".parse::<UserName>().is_err());
        assert!("user*".parse::<UserName>().is_err());
        assert!("user(".parse::<UserName>().is_err());
        assert!("user)".parse::<UserName>().is_err());
        assert!("user+".parse::<UserName>().is_err());
        assert!("user=".parse::<UserName>().is_err());
        assert!("user[".parse::<UserName>().is_err());
        assert!("user]".parse::<UserName>().is_err());
        assert!("user{".parse::<UserName>().is_err());
        assert!("user}".parse::<UserName>().is_err());
        assert!("user|".parse::<UserName>().is_err());
        assert!("user\\".parse::<UserName>().is_err());
        assert!("user:".parse::<UserName>().is_err());
        assert!("user;".parse::<UserName>().is_err());
        assert!("user\"".parse::<UserName>().is_err());
        assert!("user'".parse::<UserName>().is_err());
        assert!("user<".parse::<UserName>().is_err());
        assert!("user>".parse::<UserName>().is_err());
        assert!("user,".parse::<UserName>().is_err());
        assert!("user?".parse::<UserName>().is_err());
        assert!("user/".parse::<UserName>().is_err());
        assert!("user~".parse::<UserName>().is_err());
        assert!("user`".parse::<UserName>().is_err());
    }

    #[test]
    fn test_boundary_lengths() {
        // Exactly 4 characters (minimum)
        assert!("test".parse::<UserName>().is_ok());
        assert!("1234".parse::<UserName>().is_ok());
        assert!("abcd".parse::<UserName>().is_ok());

        // Exactly 32 characters (maximum)
        let max_length = "a".repeat(32);
        assert_eq!(max_length.len(), 32);
        assert!(max_length.parse::<UserName>().is_ok());

        // Just over 32 characters
        let over_max = "a".repeat(33);
        assert_eq!(over_max.len(), 33);
        assert!(over_max.parse::<UserName>().is_err());

        // Just under 4 characters
        assert!("abc".parse::<UserName>().is_err());
    }

    #[test]
    fn test_default() {
        let default_username = UserName::default();
        assert_eq!(default_username.to_string(), "admin");
        assert_eq!(*default_username, "admin");
    }

    #[test]
    fn test_display() {
        let username = UserName("testuser".to_string());
        assert_eq!(username.to_string(), "testuser");
        assert_eq!(format!("{}", username), "testuser");
    }

    #[test]
    fn test_deref() {
        let username = UserName("testuser".to_string());
        assert_eq!(username.len(), 8);
        assert_eq!(username.chars().count(), 8);
        assert!(username.starts_with("test"));
        assert!(username.ends_with("user"));
    }

    #[test]
    fn test_equality() {
        let username1 = UserName("testuser".to_string());
        let username2 = UserName("testuser".to_string());
        let username3 = UserName("different".to_string());

        assert_eq!(username1, username2);
        assert_ne!(username1, username3);
    }
}

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

    #[cfg(feature = "server")]
    pub network: sandpolis_instance::network::NetworkLayer,
}

impl UserLayer {
    pub async fn new(
        instance: InstanceLayer,
        database: DatabaseLayer,
        network: sandpolis_instance::network::NetworkLayer,
    ) -> Result<Self> {
        debug!("Initializing user layer");
        let user_layer = Self {
            instance,
            data: database.realm(RealmName::default())?.resident(())?,
            #[cfg(feature = "server")]
            users: database.realm(RealmName::default())?.resident_vec(())?,
            #[cfg(feature = "server")]
            network,
            #[cfg(feature = "server")]
            jwt_keys: {
                let mut jwt_keys = HashMap::new();
                // TODO all realms
                let db = database.realm(RealmName::default())?;
                let rw = db.rw_transaction()?;
                let secrets: Vec<server::ServerJwtSecret> =
                    rw.scan().primary()?.all()?.collect::<Result<Vec<_>, _>>()?;

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

pub enum UserPermission {
    Create,
    List,
    Delete,
}

#[derive(Serialize, Deserialize, PartialEq, Clone, Debug)]
pub struct ClientAuthToken(pub String);
