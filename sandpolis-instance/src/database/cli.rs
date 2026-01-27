use clap::Parser;

#[derive(Parser, Debug, Clone, Default)]
pub struct DatabaseCommandLine {
    /// Keep the database entirely in-memory, losing everything when the program exits.
    #[clap(long, num_args = 0)]
    pub ephemeral: bool,
}
