use crate::messages::{PingRequest, PingResponse};
use crate::{InstanceConnection, NetworkLayer, RequestResult};
use axum::extract::State;
use axum::extract::{self, WebSocketUpgrade};
use axum::http::StatusCode;

/// Handle an application-level "ping" or traceroute.
#[axum_macros::debug_handler]
pub async fn ping(
    state: State<NetworkLayer>,
    extract::Json(_): extract::Json<PingRequest>,
) -> RequestResult<PingResponse> {
    todo!()
}
