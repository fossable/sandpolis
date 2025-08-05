use serde::Deserialize;
use serde::Serialize;

// TODO allow embedded config

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct AgentLayerConfig {
    /// Prohibits the agent from all write operations (including self upgrades).
    ///
    /// This optional security feature is intended to mitigate the risk of a
    /// compromised server affecting agents.
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
