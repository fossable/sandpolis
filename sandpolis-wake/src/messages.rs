use crate::WakeAction;
use serde::Deserialize;
use serde::Serialize;

/// Request that the agent alter its power state.
#[derive(Serialize, Deserialize)]
pub struct WakeRequest {
    /// Type of power operation
    pub action: WakeAction,

    /// When to initiate the operation
    pub schedule: Option<String>,
}

#[derive(Serialize, Deserialize)]
pub enum WakeResponse {
    Ok,
    Failed(String),
}
