use clap::Parser;

/// Flags every instance shares. They are `global` so they parse on either side
/// of the subcommand, since they describe the process rather than the instance.
#[derive(Parser, Debug, Clone)]
pub struct InstanceCommandLine {
    /// Enable debug mode ($S7S_DEBUG)
    #[clap(long, num_args = 0, default_value_t = false, global = true)]
    pub debug: bool,

    /// Enable trace mode ($S7S_TRACE)
    #[clap(long, num_args = 0, default_value_t = false, global = true)]
    pub trace: bool,
}
