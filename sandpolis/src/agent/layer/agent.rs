use axum::{
    extract::{self, State},
    Json,
};
use axum_macros::debug_handler;

use crate::{
    agent::AgentState,
    core::layer::agent::{PowerRequest, PowerResponse},
};

#[debug_handler]
async fn power(
    state: State<AgentState>,
    extract::Json(request): extract::Json<PowerRequest>,
) -> Result<Json<PowerResponse>, Json<PowerResponse>> {
    // libc::reboot
    todo!()
}
