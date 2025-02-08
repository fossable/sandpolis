use crate::ServerAddress;
use clap::Parser;

#[derive(Parser, Debug, Clone)]
pub struct NetworkCommandLine {
    /// Servers (address:port) to connect.
    ///
    /// For GS servers, connections will be established to all given values at
    /// the same time. For LS servers, agents, and clients, only one connection
    /// can be maintained at a time.
    #[clap(long)]
    pub server: Option<Vec<ServerAddress>>,
}
