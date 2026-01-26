use crate::AgentLayer;
use crate::messages::{UninstallRequest, UninstallResponse, UpdateRequest, UpdateResponse};
use axum::extract;
use axum::extract::State;
use sandpolis_network::RequestResult;

#[derive(Serialize, Deserialize)]
pub struct UninstallRequest;

#[derive(Serialize, Deserialize)]
pub enum UninstallResponse {}

/// Uninstall the agent.
#[axum_macros::debug_handler]
pub async fn uninstall(
    state: State<AgentLayer>,
    extract::Json(request): extract::Json<UninstallRequest>,
) -> RequestResult<UninstallResponse> {
    todo!()
}
