use clap::Parser;

#[derive(Parser, Debug, Clone, Default)]
pub struct DatabaseCommandLine {
    /// Don't persist any data
    #[clap(long, num_args = 0)]
    pub ephemeral: bool,
}
