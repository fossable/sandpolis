/// Request an IPMI command be executed
pub struct IpmiCommandRequest {
    /// The IPMI command
    pub command: String,
}

pub struct IpmiCredentials {
    pub username: String,
    pub password: String,
}
