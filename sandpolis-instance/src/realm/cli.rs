use clap::Parser;
use std::path::PathBuf;

#[derive(Parser, Debug, Clone, Default)]
pub struct RealmCommandLine {
    /// Path to an external realm cert. May be given multiple times.
    #[clap(long = "realm-cert")]
    pub realm_cert: Vec<PathBuf>,
}
