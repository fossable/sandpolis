use crate::AgentLayer;
use crate::messages::{UninstallRequest, UninstallResponse, UpdateRequest, UpdateResponse};
use axum::extract;
use axum::extract::State;
use sandpolis_instance::network::RequestResult;

#[derive(Serialize, Deserialize)]
pub struct UpdateRequest;

#[derive(Serialize, Deserialize)]
pub enum UpdateResponse {}

/// Update the agent.
#[axum_macros::debug_handler]
pub async fn update(
    state: State<AgentLayer>,
    extract::Json(request): extract::Json<UpdateRequest>,
) -> RequestResult<UpdateResponse> {
    todo!()
}
