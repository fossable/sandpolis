use std::path::PathBuf;

use clap::Parser;

#[derive(Parser, Debug, Clone, Default)]
pub struct AgentCommandLine {
    /// Prohibits all write operations
    #[clap(long, default_value_t = false)]
    pub read_only: bool,

    /// Instead of maintaining a persistent connection, poll the server on this cron expression
    pub poll: Option<String>,

    /// Agent socket
    #[clap(long)]
    // TODO enforce .sock
    //, value_parser = parse_storage_dir, default_value = default_storage_dir().into_os_string())]
    pub agent_socket: PathBuf,
}
