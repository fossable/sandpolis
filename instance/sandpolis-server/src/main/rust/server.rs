pub struct Server {
    /// The server's instance identifier
    pub iid: String,

    /// The server's corresponding database URL
    pub db_url: String,

    /// The server's corresponding database username
    pub db_user: String,

    /// The server's address and port
    pub address: String,

    /// The server's gateway instance if it's in the local strata
    pub gateway_iid: String,

    /// The server's peers if it's in the global strata
    pub gs_iid: Vec<String>,
}
