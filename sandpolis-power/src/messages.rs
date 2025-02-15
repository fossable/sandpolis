use crate::PowerAction;
use serde::Deserialize;
use serde::Serialize;

/// Request that the agent alter its power state.
#[derive(Serialize, Deserialize)]
pub struct PowerRequest {
    /// Type of power operation
    pub action: PowerAction,

    /// When to initiate the operation
    pub schedule: Option<String>,
}

#[derive(Serialize, Deserialize)]
pub enum PowerResponse {
    Ok,
    Failed(String),
}
