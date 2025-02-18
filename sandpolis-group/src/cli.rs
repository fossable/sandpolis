use clap::Parser;
use std::path::PathBuf;

#[derive(Parser, Debug, Clone)]
pub struct GroupCommandLine {
    /// Path to authentication certificate which will be installed into the database.
    /// Subsequent runs don't require this option.
    #[clap(long)]
    certificate: Option<PathBuf>,
}
