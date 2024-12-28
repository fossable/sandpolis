
#[derive(Clone, Serialize, Deserialize, Validate)]
pub struct UserData {
    /// Unchangable username
    #[validate(length(min = 4), length(max = 20))]
    pub username: String,

    /// Whether the user is an admin
    pub admin: bool,

    /// Email address
    #[validate(email)]
    pub email: Option<String>,

    /// Phone number
    #[validate(phone)]
    pub phone: Option<String>,

    pub expiration: Option<i64>,
}

/// Create a new user account.
pub struct CreateUserRequest {

    pub data: UserData,

    /// Initial password as unsalted hash
    pub password: String,

    /// TOTP secret URL
    pub totp_secret: Option<URL>,
}

pub enum CreateUserResponse {
    Ok,
}

message GetUserResponse {

    // The user's immutable username
    string username = 1;

    // The user's optional email
    string email = 2;

    // The user's optional phone number
    string phone = 3;

    // The user's optional expiration timestamp
    int64 expiration = 4;
}

message GetUsersResponse {
    repeated GetUserResponse user = 1;
}

/// Update an existing user account.
pub struct UpdateUserRequest {

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
