use clap::Parser;

pub mod api;
pub mod core;

#[derive(Parser, Debug, Clone)]
#[clap(author, version, about, long_about = None)]
pub struct CommandLine {
    /// Servers (address:port) to connect
    pub server: Option<Vec<String>>,

    #[cfg(feature = "server")]
    #[clap(flatten)]
    pub server_args: crate::server::ServerCommandLine,

    #[cfg(feature = "client")]
    #[clap(flatten)]
    pub client_args: crate::client::ClientCommandLine,

    #[cfg(feature = "agent")]
    #[clap(flatten)]
    pub agent_args: crate::agent::AgentCommandLine,
}

#[cfg(feature = "agent")]
pub mod agent;

#[cfg(feature = "server")]
pub mod server;

#[cfg(feature = "client")]
pub mod client;
