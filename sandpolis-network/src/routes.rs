use crate::messages::{PingRequest, PingResponse};
use crate::{NetworkLayer, RequestResult};
use axum::extract;
use axum::extract::State;

/// Send an application-level "ping" or traceroute.
#[axum_macros::debug_handler]
pub async fn ping(
    state: State<NetworkLayer>,
    extract::Json(_): extract::Json<PingRequest>,
) -> RequestResult<PingResponse> {
    todo!()
}
