use super::ShellCommand;
use crate::{DiscoveredShell, ShellType};
use anyhow::Result;
use regex::Regex;
use sandpolis_database::Resident;
use sandpolis_macros::Stream;
use sandpolis_network::StreamResponder;
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

/// Register a scheduled command to execute in a shell.
#[derive(Serialize, Deserialize)]
pub struct ShellScheduleRequest {
    /// Shell executable to use for request
    pub shell: PathBuf,

    /// Command to execute in a new shell
    pub command: ShellCommand,

    /// Execution timeout in seconds
    pub timeout: u64,
}

/// Request message for shell execute streams.
#[derive(Serialize, Deserialize)]
pub struct ShellExecuteStreamRequest {
    /// Shell executable to use for request
    pub shell: PathBuf,

    /// Command to execute in a new shell
    pub command: ShellCommand,

    /// Execution timeout in seconds
    pub timeout: u64,

    /// Whether process output will be returned
    pub capture_output: bool,
}

/// Response message for shell execute streams.
#[derive(Serialize, Deserialize)]
pub enum ShellExecuteStreamResponse {
    Done {
        /// Process exit code
        exit_code: i32,

        /// Execution duration in seconds
        duration: f64,
        // TODO cgroup-y info like max memory, cpu time, etc
    },
    Progress {
        /// Process output on all descriptors
        output: HashMap<i32, Vec<u8>>,
    },
    Failed,
    NotFound,
    Timeout,
}

// TODO via database updates instead?
#[derive(Serialize, Deserialize)]
pub struct ShellListRequest;

/// Stream that executes a single command and then terminates.
#[derive(Stream)]
pub struct ShellExecuteStreamResponder;

impl StreamResponder for ShellExecuteStreamResponder {
    type In = ShellExecuteStreamRequest;
    type Out = ShellExecuteStreamResponse;

    async fn on_message(&self, request: Self::In, sender: Sender<Self::Out>) -> Result<()> {
        let mut cmd = Command::new(request.shell).spawn()?;

        if request.capture_output {
            // TODO progress
            match timeout(Duration::from_secs(request.timeout), cmd.wait_with_output()).await {
                Ok(output) => todo!(),
                Err(_) => sender.send(ShellExecuteStreamResponse::Timeout).await?,
            }
        } else {
            match timeout(Duration::from_secs(request.timeout), cmd.wait()).await {
                Ok(exit_status) => {
                    sender
                        .send(ShellExecuteStreamResponse::Done {
                            exit_code: exit_status?.code().unwrap_or(-1),
                            duration: todo!(),
                        })
                        .await?
                }
                Err(_) => sender.send(ShellExecuteStreamResponse::Timeout).await?,
            }
        }

        Ok(())
    }
}
