use clap::Parser;

pub mod api;
pub mod core;

/// Build info
pub mod built_info {
    include!(concat!(env!("OUT_DIR"), "/built.rs"));
}

#[derive(Parser, Debug, Clone)]
#[clap(author, version, about, long_about = None)]
pub struct CommandLine {
    #[cfg(feature = "server")]
    #[clap(flatten)]
    pub server_args: crate::server::ServerCommandLine,

    #[cfg(feature = "client")]
    #[clap(flatten)]
    pub client_args: crate::client::ClientCommandLine,

    #[cfg(feature = "agent")]
    #[clap(flatten)]
    pub agent_args: crate::agent::AgentCommandLine,

    /// Servers (address:port) to connect
    #[clap(long)]
    pub server: Option<Vec<String>>,

    #[clap(long, num_args = 0, default_value_t = false)]
    pub debug: bool,

    #[clap(long, num_args = 0, default_value_t = false)]
    pub trace: bool,
}

#[cfg(feature = "agent")]
pub mod agent;

#[cfg(feature = "server")]
pub mod server;

#[cfg(feature = "client")]
pub mod client;
