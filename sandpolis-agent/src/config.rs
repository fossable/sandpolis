use serde::Deserialize;
use serde::Serialize;

// TODO allow embedded config

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct AgentLayerConfig {
    /// Prohibits the agent from all write operations (including self upgrades).
    ///
    /// This optional security feature is intended to mitigate the risk of a
    /// compromised server negatively impacting agents.
    pub read_only: bool,

    /// Server URLs the agent should maintain connections to. Each entry is
    /// parsed as a `ServerUrl` at runtime; an unparseable entry is skipped
    /// with a warning.
    #[serde(default)]
    pub servers: Vec<String>,
}

impl Default for AgentLayerConfig {
    fn default() -> Self {
        Self {
            read_only: false,
            servers: Vec::new(),
        }
    }
}
