use anyhow::anyhow;
use anyhow::bail;
use anyhow::Result;
use serde::Deserialize;
use serde::Serialize;
use std::path::PathBuf;

// TODO allow embedded config

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct AgentLayerConfig {
    /// Prohibits all write operations
    pub read_only: bool,

    /// Agent socket that allows external processes to invoke agent actions.
    pub socket: PathBuf,

    /// Read config options from the environment
    pub env_overrides: bool,
}

impl Default for AgentLayerConfig {
    fn default() -> Self {
        Self {
            read_only: false,
            socket: "/tmp/agent.sock".into(),
            env_overrides: true,
        }
    }
}

impl AgentLayerConfig {
    pub fn clear_socket_path(&self) -> Result<()> {
        if self
            .socket
            .extension()
            .ok_or(anyhow!("Socket path must have an extension"))?
            != "sock"
        {
            bail!("Socket path must end with .sock");
        }

        // If the parent directory doesn't exist, create it
        if !std::fs::exists(
            self.socket
                .parent()
                .ok_or(anyhow!("Socket must be within a directory"))?,
        )? {
            std::fs::create_dir_all(&self.socket.parent().unwrap())?;
        }

        Ok(())
    }
}
