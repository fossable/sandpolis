
// Request that a new session be created. Any previous sessions associated with the
// instance are invalidated.
message RQ_Session {

    // The UUID of the requesting instance
    string instance_uuid = 1;

    // The instance type of the requesting instance
    //core.foundation.InstanceType instance_type = 2;
}

// Respond to a session request with a successful result.
message RS_Session {

    // A SID for the requesting instance
    int32 instance_sid  = 1;

    // The SID of the server
    int32 server_sid = 2;

    // The UUID of the server
    string server_uuid = 3;
}

// Request to authenticate the current session.
//
// Sources      : agent
// Destinations : server
//
message RQ_AuthSession {

    oneof auth_method {

        // The group password
        string password = 1;

        // The authentication token
        string token = 2;
    }
}

enum RS_AuthSession {
    AUTH_SESSION_OK = 0;
    AUTH_SESSION_FAILED = 1;
}

// Request to refresh an agent's authentication certificate.
//
// Sources      : server
// Destinations : agent
//
message RQ_RefreshAuthCertificate {

    // The new "client" certificate
    bytes certificate = 1;
}

// Request a login from the server
message PostLoginRequest {

    // The login username
    string username = 1;

    // The password
    string password = 2;

    // Time-based One-Time Password token
    int32 token = 3;
}

enum PostLoginResponse {
    LOGIN_OK = 0;

    LOGIN_FAILED = 1;
    LOGIN_FAILED_EXPIRED_USER = 2;

    // The given username was not valid
    LOGIN_INVALID_USERNAME = 3;

    // The given password was not valid
    LOGIN_INVALID_PASSWORD = 4;

    // The given token
    LOGIN_INVALID_TOKEN = 5;

}
