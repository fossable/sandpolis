use crate::{DiscoveredShell, ShellType};
use anyhow::Result;
use regex::Regex;
use sandpolis_instance::database::Resident;
use sandpolis_macros::Stream;
use sandpolis_instance::network::StreamResponder;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::path::PathBuf;
use std::time::Duration;
use tokio::sync::RwLock;
use tokio::sync::mpsc::Sender;
use tokio::time::timeout;
use tokio::{
    io::{AsyncReadExt, AsyncWriteExt},
    process::{Child, Command},
};
use tracing::{debug, trace};

/// Request message for shell session streams.
#[derive(Serialize, Deserialize)]
pub enum ShellSessionStreamRequest {
    /// Requester wants to start the stream
    Start {
        /// Path to the shell executable
        path: PathBuf,

        // TODO request permissions
        // Permission permission = 3;
        /// Additional environment variables
        environment: HashMap<String, String>,

        /// Number of rows to request
        rows: u32,

        /// Number of columns to request
        cols: u32,
    },
    /// Requester has stdin data
    Stdin { data: Vec<u8> },
    /// Requester changed the size of the terminal
    Resize {
        /// Update the number of rows
        rows: u32,

        /// Update the number of columns
        cols: u32,
    },
}

/// Event containing standard-output and standard-error.
#[derive(Serialize, Deserialize)]
pub struct ShellSessionStreamResponse {
    pub stdout: Vec<u8>,
    pub stderr: Vec<u8>,
}

/// Stream that runs a bidirectional shell session.
#[derive(Stream, Default)]
pub struct ShellSessionStreamResponder {
    pub process: RwLock<Option<Child>>,
    pub stdin: RwLock<Option<tokio::process::ChildStdin>>,
}

impl StreamResponder for ShellSessionStreamResponder {
    type In = ShellSessionStreamRequest;
    type Out = ShellSessionStreamResponse;

    async fn on_message(&self, request: Self::In, sender: Sender<Self::Out>) -> Result<()> {
        match request {
            ShellSessionStreamRequest::Start {
                path,
                mut environment,
                rows,
                cols,
            } => {
                // Add a default for TERM
                if !environment.contains_key("TERM") {
                    environment.insert("TERM".to_string(), "screen-256color".to_string());
                }

                // Add a default for rows/cols
                let rows = if rows == 0 { 120 } else { rows };
                let cols = if cols == 0 { 80 } else { cols };

                environment.insert("ROWS".to_string(), rows.to_string());
                environment.insert("COLS".to_string(), cols.to_string());

                // Spawn the process and extract stdin/stdout
                let mut child = Command::new(&path)
                    .envs(environment)
                    .stdin(std::process::Stdio::piped())
                    .stdout(std::process::Stdio::piped())
                    .stderr(std::process::Stdio::piped())
                    .spawn()?;
                let mut stdout = child.stdout.take().unwrap();
                let stdin = child.stdin.take().unwrap();

                // Store the child process and stdin for later use
                *self.process.write().await = Some(child);
                *self.stdin.write().await = Some(stdin);

                // Read stdout in a loop (lock is released before the loop)
                loop {
                    let mut response = ShellSessionStreamResponse {
                        stdout: Vec::new(),
                        stderr: Vec::new(),
                    };
                    match stdout.read_buf(&mut response.stdout).await {
                        Ok(0) => break, // EOF
                        Ok(_) => {
                            if sender.send(response).await.is_err() {
                                break;
                            }
                        }
                        Err(_) => break,
                    }
                }
            }
            ShellSessionStreamRequest::Stdin { data } => {
                if let Some(ref mut stdin) = *self.stdin.write().await {
                    let _ = stdin.write_all(&data).await;
                }
            }
            ShellSessionStreamRequest::Resize { rows, cols } => todo!(),
        }
        Ok(())
    }
}

impl Drop for ShellSessionStreamResponder {
    fn drop(&mut self) {
        debug!("Killing child process");
        if let Some(ref mut child) = *self.process.get_mut() {
            let _ = child.start_kill();
        }

        // self.data.update(ShellSessionDelta::ended);
    }
}

#[cfg(test)]
mod test_shell_session {
    use super::*;
    use std::collections::HashMap;
    use std::path::PathBuf;
    use std::sync::Arc;
    use tokio::sync::mpsc;

    #[tokio::test]
    async fn test_start_and_receive_output() {
        let responder = Arc::new(ShellSessionStreamResponder::default());
        let (tx, mut rx) = mpsc::channel::<ShellSessionStreamResponse>(32);

        // Start a simple echo command
        let request = ShellSessionStreamRequest::Start {
            path: PathBuf::from("/bin/sh"),
            environment: HashMap::new(),
            rows: 24,
            cols: 80,
        };

        // Run on_message in a separate task since it blocks reading stdout
        let responder_clone = responder.clone();
        let handle = tokio::spawn(async move { responder_clone.on_message(request, tx).await });

        // Send a command via stdin
        tokio::time::sleep(tokio::time::Duration::from_millis(100)).await;
        let stdin_request = ShellSessionStreamRequest::Stdin {
            data: b"echo hello\nexit\n".to_vec(),
        };
        responder
            .on_message(stdin_request, mpsc::channel(1).0)
            .await
            .unwrap();

        // Collect output
        let mut output = Vec::new();
        while let Ok(response) =
            tokio::time::timeout(tokio::time::Duration::from_secs(2), rx.recv()).await
        {
            match response {
                Some(resp) => output.extend(resp.stdout),
                None => break,
            }
        }

        // Wait for the handler to finish
        let _ = handle.await;

        // Verify we got some output containing "hello"
        let output_str = String::from_utf8_lossy(&output);
        assert!(
            output_str.contains("hello"),
            "Expected 'hello' in output, got: {}",
            output_str
        );
    }

    #[tokio::test]
    async fn test_stdin_before_start_is_noop() {
        let responder = ShellSessionStreamResponder::default();
        let (tx, _rx) = mpsc::channel::<ShellSessionStreamResponse>(32);

        // Sending stdin before starting should not panic
        let request = ShellSessionStreamRequest::Stdin {
            data: b"test".to_vec(),
        };
        let result = responder.on_message(request, tx).await;
        assert!(result.is_ok());
    }

    #[tokio::test]
    async fn test_environment_defaults() {
        let responder = Arc::new(ShellSessionStreamResponder::default());
        let (tx, mut rx) = mpsc::channel::<ShellSessionStreamResponse>(32);

        // Start with empty environment and zero rows/cols to test defaults
        let request = ShellSessionStreamRequest::Start {
            path: PathBuf::from("/bin/sh"),
            environment: HashMap::new(),
            rows: 0,
            cols: 0,
        };

        let responder_clone = responder.clone();
        let handle = tokio::spawn(async move { responder_clone.on_message(request, tx).await });

        // Wait for shell to start
        tokio::time::sleep(tokio::time::Duration::from_millis(100)).await;

        // Echo environment variables to verify they were set
        let stdin_request = ShellSessionStreamRequest::Stdin {
            data: b"echo TERM=$TERM ROWS=$ROWS COLS=$COLS\nexit\n".to_vec(),
        };
        responder
            .on_message(stdin_request, mpsc::channel(1).0)
            .await
            .unwrap();

        // Collect output
        let mut output = Vec::new();
        while let Ok(response) =
            tokio::time::timeout(tokio::time::Duration::from_secs(2), rx.recv()).await
        {
            match response {
                Some(resp) => output.extend(resp.stdout),
                None => break,
            }
        }

        let _ = handle.await;

        let output_str = String::from_utf8_lossy(&output);
        assert!(
            output_str.contains("TERM=screen-256color"),
            "Expected default TERM, got: {}",
            output_str
        );
        assert!(
            output_str.contains("ROWS=120"),
            "Expected default ROWS=120, got: {}",
            output_str
        );
        assert!(
            output_str.contains("COLS=80"),
            "Expected default COLS=80, got: {}",
            output_str
        );
    }

    #[tokio::test]
    async fn test_environment_custom_values() {
        let responder = Arc::new(ShellSessionStreamResponder::default());
        let (tx, mut rx) = mpsc::channel::<ShellSessionStreamResponse>(32);

        // Start with custom environment
        let mut env = HashMap::new();
        env.insert("TERM".to_string(), "xterm-256color".to_string());
        env.insert("MY_VAR".to_string(), "custom_value".to_string());

        let request = ShellSessionStreamRequest::Start {
            path: PathBuf::from("/bin/sh"),
            environment: env,
            rows: 50,
            cols: 100,
        };

        let responder_clone = responder.clone();
        let handle = tokio::spawn(async move { responder_clone.on_message(request, tx).await });

        tokio::time::sleep(tokio::time::Duration::from_millis(100)).await;

        let stdin_request = ShellSessionStreamRequest::Stdin {
            data: b"echo TERM=$TERM ROWS=$ROWS COLS=$COLS MY_VAR=$MY_VAR\nexit\n".to_vec(),
        };
        responder
            .on_message(stdin_request, mpsc::channel(1).0)
            .await
            .unwrap();

        let mut output = Vec::new();
        while let Ok(response) =
            tokio::time::timeout(tokio::time::Duration::from_secs(2), rx.recv()).await
        {
            match response {
                Some(resp) => output.extend(resp.stdout),
                None => break,
            }
        }

        let _ = handle.await;

        let output_str = String::from_utf8_lossy(&output);
        // Custom TERM should be preserved (not overwritten)
        assert!(
            output_str.contains("TERM=xterm-256color"),
            "Expected custom TERM, got: {}",
            output_str
        );
        // Custom rows/cols should be used
        assert!(
            output_str.contains("ROWS=50"),
            "Expected ROWS=50, got: {}",
            output_str
        );
        assert!(
            output_str.contains("COLS=100"),
            "Expected COLS=100, got: {}",
            output_str
        );
        // Custom env var should be passed through
        assert!(
            output_str.contains("MY_VAR=custom_value"),
            "Expected MY_VAR=custom_value, got: {}",
            output_str
        );
    }
}
