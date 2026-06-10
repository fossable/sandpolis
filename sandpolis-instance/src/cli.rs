use clap::Parser;

#[derive(Parser, Debug, Clone)]
pub struct InstanceCommandLine {
    /// Enable debug mode ($S7S_DEBUG)
    #[clap(long, num_args = 0, default_value_t = false)]
    pub debug: bool,

    /// Enable trace mode ($S7S_TRACE)
    #[clap(long, num_args = 0, default_value_t = false)]
    pub trace: bool,

    /// Disable the local admin socket. This socket allows modification to the
    /// running process, so misconfiguring the file permissions on it may be
    /// a security risk.
    #[clap(long, num_args = 0, default_value_t = false)]
    pub no_admin_socket: bool,
}
