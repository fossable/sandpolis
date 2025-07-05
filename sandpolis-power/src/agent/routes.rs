use crate::messages::PowerRequest;
use crate::messages::PowerResponse;
use crate::PowerLayer;
use axum::extract;
use axum::extract::State;
use axum::Json;

/// Modify the agent's current power state (shutdown, reboot, etc).
#[axum_macros::debug_handler]
async fn power(
    state: State<PowerLayer>,
    extract::Json(request): extract::Json<PowerRequest>,
) -> Result<Json<PowerResponse>, Json<PowerResponse>> {
    // libc::reboot
    todo!()
}
