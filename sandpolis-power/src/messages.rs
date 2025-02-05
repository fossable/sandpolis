use serde::Deserialize;
use serde::Serialize;

/// Request that the agent alter its power state.
#[derive(Serialize, Deserialize)]
pub enum PowerRequest {
    Poweroff,
    Reboot,
}

#[derive(Serialize, Deserialize)]
pub enum PowerResponse {
    Ok,
    Failed(String),
}
