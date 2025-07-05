use super::ShellSessionData;
use super::messages::{ShellSessionOutputEvent, ShellSessionRequest};
use crate::{DiscoveredShell, ShellType};
use anyhow::Result;
use axum::extract::ws::{Message, WebSocket};
use futures::{SinkExt, StreamExt};
use regex::Regex;
use sandpolis_database::Resident;
use tokio::{
    io::{AsyncReadExt, AsyncWriteExt},
    process::{Child, Command},
};
use tracing::{debug, trace};

pub mod routes;

pub struct ShellSession {
    // pub id: StreamId,
    pub data: Resident<ShellSessionData>,
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

impl DiscoveredShell {
    pub async fn scan() -> Result<Vec<DiscoveredShell>> {
        let mut shells = Vec::new();

        // Search for bash
        match Command::new("bash").arg("--version").output().await {
            Ok(output) => match String::from_utf8(output.stdout) {
                Ok(stdout) => {
                    if let Some(m) =
                        Regex::new(r"version ([1-9]+\.[0-9]+\.[0-9]+\S*)")?.captures(&stdout)
                    {
                        shells.push(DiscoveredShell {
                            shell_type: ShellType::Bash,
                            location: todo!(),
                            version: todo!(),
                        })
                    }
                }
                Err(_) => todo!(),
            },
            Err(_) => trace!("Bash shell not found"),
        };

        Ok(shells)
    }
}

#[cfg(test)]
mod test_discovered_shell {
    #[test]
    pub fn test_scan() {
        // Assume at least one shell is available
        assert!(super::DiscoveredShell::scan().unwrap().len() > 0);
    }
}
