use super::ShellCommand;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::path::PathBuf;

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
