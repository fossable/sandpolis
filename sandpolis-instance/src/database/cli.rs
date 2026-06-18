use clap::Parser;
use std::path::PathBuf;

#[derive(Parser, Debug, Clone, Default)]
pub struct DatabaseCommandLine {
    /// Directory where the database is stored.
    #[clap(long)]
    pub data_dir: Option<PathBuf>,

    /// Keep the database entirely in-memory, losing everything when the program exits.
    #[clap(long, num_args = 0)]
    pub ephemeral: bool,
}
