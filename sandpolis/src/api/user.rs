
// Create a new user account.
message PostUserRequest {

    // The user's immutable username
    string username = 1;

    // The user's password
    string password = 2;

    // The user's token for TOTP verification
    string totp_token = 3;

    // The user's optional email
    string email = 4;

    // The user's optional phone number
    string phone = 5;

    // The user's optional expiration timestamp
    int64 expiration = 6;
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

// Update an existing user account.
message PutUserRequest {

    // The user's new password
    string password = 1;

    // The user's new email
    string email = 2;

    // The user's new phone number
    string phone = 3;

    // The user's new expiration timestamp
    int64 expiration = 4;
}
