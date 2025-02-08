use crate::messages::{FsDeleteRequest, FsDeleteResponse, FsSessionRequest};
use crate::FilesystemLayer;
use axum::extract::State;
use axum::extract::{self, WebSocketUpgrade};
use axum::http::StatusCode;
use sandpolis_network::RequestResult;

#[axum_macros::debug_handler]
pub async fn session(
    state: State<FilesystemLayer>,
    ws: WebSocketUpgrade,
    extract::Json(request): extract::Json<FsSessionRequest>,
) -> Result<(), StatusCode> {
    // let session = ShellSession::new(request).map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    // ws.on_upgrade(move |socket| session.run(socket));

    Ok(())
}

#[axum_macros::debug_handler]
pub async fn delete(
    state: State<FilesystemLayer>,
    extract::Json(request): extract::Json<FsDeleteRequest>,
) -> RequestResult<FsDeleteResponse> {
    todo!()
}
