use serde::Deserialize;
use serde::Serialize;

// TODO convert to CLI args and add configure agents here

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

    /// When set, the agent connects in "polling" mode: instead of holding a
    /// persistent connection, it only checks in with its servers on this
    /// schedule. Best when latency is not important (low-power or low-footprint
    /// agents). When unset, the agent stays continuously connected.
    #[serde(default)]
    pub poll: Option<PollConfig>,
}

/// Configures the agent's "polling" connection mode.
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct PollConfig {
    /// Cron expression describing when the agent connects to check in, e.g.
    /// `"0 */5 * * * *"` for every five minutes.
    pub schedule: String,

    /// How long the agent stays connected during each check-in window, in
    /// seconds. The server pulls the agent's accumulated data and delivers any
    /// pending work during this window before the connection is closed again.
    #[serde(default = "PollConfig::default_timeout_secs")]
    pub timeout_secs: u64,
}

impl PollConfig {
    fn default_timeout_secs() -> u64 {
        30
    }
}

impl Default for AgentLayerConfig {
    fn default() -> Self {
        Self {
            read_only: false,
            servers: Vec::new(),
            poll: None,
        }
    }
}
