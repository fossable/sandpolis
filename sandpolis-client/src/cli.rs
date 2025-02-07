use clap::Parser;

#[derive(Parser, Debug, Clone, Default)]
pub struct ClientCommandLine {
    /// Run client graphical UI (even if invoked from a terminal)
    #[cfg(feature = "client-gui")]
    #[clap(long, conflicts_with = "tui")]
    pub gui: bool,

    /// Run client terminal UI
    #[cfg(feature = "client-tui")]
    #[clap(long, conflicts_with = "gui")]
    pub tui: bool,
}
