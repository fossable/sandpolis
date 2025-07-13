use clap::Parser;
use std::path::PathBuf;

#[derive(Parser, Debug, Clone, Default)]
pub struct RealmCommandLine {
    /// Path to a realm cert to import. Subsequent runs don't require this
    /// option.
    #[clap(long)]
    pub agent_cert: Option<PathBuf>,

    /// Path to a realm cert to import. Subsequent runs don't require this
    /// option.
    #[clap(long)]
    pub client_cert: Option<PathBuf>,
}
