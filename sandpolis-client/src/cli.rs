use clap::Parser;

#[derive(Parser, Debug, Clone, Default)]
pub struct ClientCommandLine {
    /// Run client graphical UI (even if invoked from a terminal)
    #[cfg(all(feature = "client-gui", feature = "client-tui"))]
    #[clap(long, num_args = 0, conflicts_with = "tui")]
    pub gui: bool,

    /// Run client terminal UI
    #[cfg(all(feature = "client-gui", feature = "client-tui"))]
    #[clap(long, num_args = 0, conflicts_with = "gui")]
    pub tui: bool,
}
