use serde::{Deserialize, Serialize};
use url::Url;
use validator::Validate;

#[derive(Serialize, Deserialize, Validate)]
#[cfg_attr(feature = "client", derive(bevy::prelude::Component))]
pub struct UserData {
    /// Unchangable username
    #[validate(length(min = 4, max = 20))]
    pub username: String,

    /// Whether the user is an admin
    pub admin: bool,

    /// Email address
    #[validate(email)]
    pub email: Option<String>,

    /// Phone number
    pub phone: Option<String>,

    pub expiration: Option<i64>,
}

/// Create a new user account.
#[derive(Serialize, Deserialize, Validate)]
pub struct CreateUserRequest {
    pub data: UserData,

    /// Password as unsalted hash
    pub password: String,

    /// TOTP secret URL
    pub totp_secret: Option<Url>,
}

pub enum CreateUserResponse {
    Ok,
}

pub struct GetUsersRequest {
    /// Search by username prefix
    pub username: Option<String>,

    /// Search by email prefix
    pub email: Option<String>,
}

pub enum GetUsersResponse {
    Ok(Vec<UserData>),
}

/// Update an existing user account.
pub struct UpdateUserRequest {
    /// User to edit
    pub username: String,

    /// New password
    pub password: Option<String>,

    /// New email
    pub email: Option<String>,

    /// New phone number
    pub phone: Option<String>,

    /// New expiration timestamp
    pub expiration: Option<u64>,
}

pub enum UpdateUserResponse {
    Ok,

    /// The requested user does not exist
    NotFound,
}

/// Request a login from the server
pub struct LoginRequest {
    /// User to login as
    pub username: String,

    /// Password as unsalted hash
    pub password: String,

    /// Time-based One-Time Password token
    pub totp_token: Option<String>,
}

pub enum LoginResponse {
    Ok,

    /// The request was invalid
    Invalid,

    /// The user account is expired
    Expired,

    /// The password and/or TOTP token were incorrect
    Denied,
}
