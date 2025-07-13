use serde::Deserialize;
use serde::Serialize;

// TODO allow embedded config

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct AgentLayerConfig {
    /// Prohibits all write operations
    pub read_only: bool,

    /// Read config options from the environment
    pub env_overrides: bool,
}

impl Default for AgentLayerConfig {
    fn default() -> Self {
        Self {
            read_only: false,
            env_overrides: true,
        }
    }
}
