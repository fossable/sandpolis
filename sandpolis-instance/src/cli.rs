use std::path::PathBuf;

use clap::Parser;

#[derive(Parser, Debug, Clone)]
pub struct InstanceCommandLine {
    /// Enable debug mode ($S7S_DEBUG)
    #[clap(long, num_args = 0, default_value_t = false)]
    pub debug: bool,

    /// Enable trace mode ($S7S_TRACE)
    #[clap(long, num_args = 0, default_value_t = false)]
    pub trace: bool,

    /// Configuration file path ($S7S_CONFIG)
    #[clap(long)]
    pub config: Option<PathBuf>,
}
