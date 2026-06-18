use clap::Parser;
use std::path::PathBuf;

#[derive(Parser, Debug, Clone, Default)]
pub struct RealmCommandLine {
    /// Path to a realm cert to import. May be given multiple times. The cert is
    /// loaded on every run and is required as long as the instance connects to
    /// a server.
    #[clap(long = "realm-cert")]
    pub realm_cert: Vec<PathBuf>,
}
