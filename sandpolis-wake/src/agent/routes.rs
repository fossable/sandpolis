use crate::WakeLayer;
use crate::messages::WakeRequest;
use crate::messages::WakeResponse;
use axum::Json;
use axum::extract;
use axum::extract::State;
use sandpolis_network::StreamResponder;

/// Modify the agent's current power state (shutdown, reboot, etc).
#[derive(StreamResponder)]
pub struct WakeStreamResponder;
// libc::reboot
