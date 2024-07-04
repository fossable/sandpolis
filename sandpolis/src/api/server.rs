
// Response bearing the server's banner
message GetBannerResponse {

    // Maintenance mode indicates that only admin users will be allowed to login
    bool maintenance = 1;

    // The 3-field version of the server
    string version = 2;

    // A string to display on the login screen
    string message = 3;

    // An image to display on the login screen
    bytes image = 4;
}
