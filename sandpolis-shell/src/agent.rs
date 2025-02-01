use std::{os::unix::process::CommandExt, time::Duration};

use anyhow::Result;
use axum::{
    extract::{
        self,
        ws::{Message, WebSocket, WebSocketUpgrade},
    },
    http::StatusCode,
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

use super::{
    ShellExecuteRequest, ShellExecuteResponse, ShellSessionData, ShellSessionInputEvent,
    ShellSessionOutputEvent, ShellSessionRequest,
};
use sandpolis_database::Document;

pub struct ShellSession {
    // pub id: StreamId,
    pub data: Document<ShellSessionData>,
    pub process: Child,
}

impl ShellSession {
    pub fn new(mut request: ShellSessionRequest) -> Result<Self> {
        // Add a default for TERM
        if request.environment.get("TERM").is_none() {
            request
                .environment
                .insert("TERM".to_string(), "screen-256color".to_string());
        }

        // Add a default for rows/cols
        if request.rows == 0 {
            request.rows = 120;
        }
        if request.cols == 0 {
            request.cols = 80;
        }

        request
            .environment
            .insert("ROWS".to_string(), request.rows.to_string());
        request
            .environment
            .insert("COLS".to_string(), request.cols.to_string());

        Ok(Self {
            process: Command::new(&request.path)
                .envs(request.environment)
                .spawn()?,
            data: todo!(),
        })
    }

    pub async fn run(mut self, socket: WebSocket) {
        let (mut sender, mut receiver) = socket.split();

        let mut stdin = self.process.stdin.take().unwrap();
        let mut stdin_task = tokio::spawn(async move {
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

        let mut stdout = self.process.stdout.take().unwrap();
        let mut stdout_task = tokio::spawn(async move {
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

        // self.data.update(ShellSessionDelta::ended);
    }
}

#[debug_handler]
async fn shell_session(
    // state: State<AppState>,
    ws: WebSocketUpgrade,
    extract::Json(request): extract::Json<ShellSessionRequest>,
) -> Result<(), StatusCode> {
    let session = ShellSession::new(request).map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    ws.on_upgrade(move |socket| session.run(socket));

    Ok(())
}

#[debug_handler]
async fn shell_execute(
    // state: State<AppState>,
    extract::Json(request): extract::Json<ShellExecuteRequest>,
) -> Result<Json<ShellExecuteResponse>, StatusCode> {
    let mut cmd = Command::new(request.shell)
        .spawn()
        .map_err(|_| StatusCode::NOT_FOUND)?;

    Ok(Json(if request.capture_output {
        match timeout(Duration::from_secs(request.timeout), cmd.wait_with_output()).await {
            Ok(output) => todo!(),
            Err(_) => ShellExecuteResponse::Timeout,
        }
    } else {
        match timeout(Duration::from_secs(request.timeout), cmd.wait()).await {
            Ok(exit_status) => ShellExecuteResponse::Ok {
                exit_code: exit_status
                    .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?
                    .code()
                    .unwrap_or(-1),
                duration: todo!(),
                output: todo!(),
            },
            Err(_) => ShellExecuteResponse::Timeout,
        }
    }))
}

pub fn router() -> Router<AgentState> {
    Router::new()
        .route("/execute", post(shell_execute))
        .route("/session", any(shell_session))
}
