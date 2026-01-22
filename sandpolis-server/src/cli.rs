use crate::ServerUrl;
use clap::Parser;

#[derive(Parser, Debug, Clone)]
pub struct ServerCommandLine {
    /// Server addresses ($S7S_SERVER)
    #[clap(long)]
    pub server: Option<Vec<ServerUrl>>,
}
