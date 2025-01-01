use std::{os::unix::process::CommandExt, time::Duration};

use anyhow::Result;
use axum::{
    extract::{
        self,
        ws::{Message, WebSocket, WebSocketUpgrade},
    },
    response::IntoResponse,
    routing::{any, post},
    Json, Router,
};
use axum_macros::debug_handler;
use futures::{SinkExt, StreamExt};
use tokio::{
    io::{AsyncReadExt, AsyncWriteExt},
    process::{Child, Command},
    time::timeout,
};
use tracing::debug;

use crate::core::layer::{
    network::stream::{StreamSink, StreamSource},
    shell::{
        ShellExecuteRequest, ShellExecuteResponse, ShellSessionData, ShellSessionInputEvent,
        ShellSessionOutputEvent, ShellSessionRequest,
    },
};

pub struct ShellSession {
    // pub id: StreamId,
    pub data: ShellSessionData,
    pub process: Child,
}

impl ShellSession {
    pub fn new() -> Self {}

    pub async fn run(&mut self, socket: WebSocket) {
        let (mut sender, mut receiver) = socket.split();

        let mut stdin_task = tokio::spawn(async move {
            let stdin = self.process.stdin.take().unwrap();
            while let Some(Ok(msg)) = receiver.next().await {
                match msg {
                    Message::Binary(data) => match stdin.write_all(&data).await {
                        Ok(_) => todo!(),
                        Err(_) => todo!(),
                    },
                    Message::Close(_) => break,
                    _ => {}
                }
            }
        });

        let mut stdout_task = tokio::spawn(async move {
            let stdout = self.process.stdout.take().unwrap();
            loop {
                let mut event = ShellSessionOutputEvent::default();
                match stdout.read_buf(&mut event.stdout).await {
                    Ok(_) => match sender.send(event.into()).await {
                        Ok(_) => todo!(),
                        Err(_) => todo!(),
                    },
                    Err(_) => todo!(),
                }
            }
        });

        tokio::select! {
            rv_a = (&mut stdout_task) => {
                match rv_a {
                    Ok(a) => todo!(),
                    Err(a) => todo!()
                }
                stdin_task.abort();
            },
            rv_b = (&mut stdin_task) => {
                match rv_b {
                    Ok(b) => todo!(),
                    Err(b) => todo!()
                }
                stdout_task.abort();
            }
        }
    }
}

impl Drop for ShellSession {
    fn drop(&mut self) {
        debug!("Killing child process");
        self.process.kill(); // TODO await

        self.data.ended = todo!();
    }
}

#[debug_handler]
async fn shell_session(
    // state: State<AppState>,
    ws: WebSocketUpgrade,
    extract::Json(request): extract::Json<ShellSessionRequest>,
) -> impl IntoResponse {
    let mut session = ShellSession::new();
    ws.on_upgrade(move |socket| session.run(socket))
}

#[debug_handler]
async fn shell_execute(
    // state: State<AppState>,
    extract::Json(request): extract::Json<ShellExecuteRequest>,
) -> Result<Json<ShellExecuteResponse>, axum::http::StatusCode> {
    let cmd = Command::new(request.shell)
        .spawn()
        .map_err(|_| axum::http::StatusCode::NOT_FOUND)?;

    Ok(Json(if request.capture_output {
        match timeout(Duration::from_secs(request.timeout), cmd.wait_with_output()).await {
            Ok(output) => todo!(),
            Err(_) => ShellExecuteResponse::Timeout,
        }
    } else {
        match timeout(Duration::from_secs(request.timeout), cmd.wait()).await {
            Ok(exit_status) => ShellExecuteResponse::Ok {
                exit_code: exit_status,
                duration: todo!(),
                output: todo!(),
            },
            Err(_) => ShellExecuteResponse::Timeout,
        }
    }))
}

pub fn router() -> Router {
    Router::new()
        .route("/execute", post(shell_execute))
        .route("/session", any(shell_session))
}
