use anyhow::Result;
use axum::{
    extract::{self, ws::WebSocketUpgrade},
    http::StatusCode,
    Json,
};
use sandpolis_network::RequestResult;
use std::time::Duration;
use tokio::{
    process::Command,
    time::timeout,
};

use super::ShellSession;
use crate::messages::{
    ShellExecuteRequest, ShellExecuteResponse, ShellSessionRequest,
};

#[axum_macros::debug_handler]
pub async fn session(
    // state: State<AppState>,
    ws: WebSocketUpgrade,
    extract::Json(request): extract::Json<ShellSessionRequest>,
) -> Result<(), StatusCode> {
    let session = ShellSession::new(request).map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    ws.on_upgrade(move |socket| session.run(socket));

    Ok(())
}

#[axum_macros::debug_handler]
pub async fn execute(
    // state: State<AppState>,
    extract::Json(request): extract::Json<ShellExecuteRequest>,
) -> RequestResult<ShellExecuteResponse> {
    let mut cmd = Command::new(request.shell)
        .spawn()
        .map_err(|_| Json(ShellExecuteResponse::NotFound))?;

    Ok(Json(if request.capture_output {
        match timeout(Duration::from_secs(request.timeout), cmd.wait_with_output()).await {
            Ok(output) => todo!(),
            Err(_) => ShellExecuteResponse::Timeout,
        }
    } else {
        match timeout(Duration::from_secs(request.timeout), cmd.wait()).await {
            Ok(exit_status) => ShellExecuteResponse::Ok {
                exit_code: exit_status
                    .map_err(|_| Json(ShellExecuteResponse::Failed))?
                    .code()
                    .unwrap_or(-1),
                duration: todo!(),
                output: todo!(),
            },
            Err(_) => ShellExecuteResponse::Timeout,
        }
    }))
}
