use crate::messages::{UninstallRequest, UninstallResponse, UpdateRequest, UpdateResponse};
use crate::AgentLayer;
use axum::extract;
use axum::extract::State;
use sandpolis_network::RequestResult;

/// Uninstall the agent.
#[axum_macros::debug_handler]
pub async fn uninstall(
    state: State<AgentLayer>,
    extract::Json(request): extract::Json<UninstallRequest>,
) -> RequestResult<UninstallResponse> {
    todo!()
}

/// Update the agent.
#[axum_macros::debug_handler]
pub async fn update(
    state: State<AgentLayer>,
    extract::Json(request): extract::Json<UpdateRequest>,
) -> RequestResult<UpdateResponse> {
    todo!()
}
