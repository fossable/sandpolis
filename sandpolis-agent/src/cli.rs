use anyhow::anyhow;
use anyhow::bail;
use anyhow::Result;
use clap::Parser;
use std::{path::PathBuf, str::FromStr};

fn parse_socket_path(value: &str) -> Result<PathBuf> {
    let path = PathBuf::from_str(value)?;
    if path
        .extension()
        .ok_or(anyhow!("Socket path must have an extension"))?
        != "sock"
    {
        bail!("Socket path must end with .sock");
    }

    // If the parent directory doesn't exist, create it
    if !std::fs::exists(
        path.parent()
            .ok_or(anyhow!("Socket must be within a directory"))?,
    )? {
        std::fs::create_dir_all(&path.parent().unwrap())?;
    }

    Ok(path)
}

fn default_socket_path() -> PathBuf {
    "/tmp/agent.sock".into()
}

#[derive(Parser, Debug, Clone, Default)]
pub struct AgentCommandLine {
    /// Prohibits all write operations
    #[clap(long, default_value_t = false)]
    pub read_only: bool,

    /// Instead of maintaining a persistent connection, poll the server on this cron expression
    #[clap(long)]
    pub poll: Option<String>,

    /// Agent socket
    #[clap(long, value_parser = parse_socket_path, default_value = default_socket_path().into_os_string())]
    pub agent_socket: PathBuf,
}
