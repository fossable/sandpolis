use super::ShellCommand;
use sandpolis_macros::StreamEvent;
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

/// Request to execute a command in a shell.
#[derive(Serialize, Deserialize)]
pub struct ShellExecuteRequest {
    /// Shell executable to use for request
    pub shell: PathBuf,

    /// Command to execute in a new shell
    pub command: ShellCommand,

    /// Execution timeout in seconds
    pub timeout: u64,

    /// Whether process output will be returned
    pub capture_output: bool,
}

/// Response containing execution results.
#[derive(Serialize, Deserialize)]
pub enum ShellExecuteResponse {
    Ok {
        /// Process exit code
        exit_code: i32,

        /// Execution duration in seconds
        duration: f64,

        /// Process output on all descriptors
        output: HashMap<i32, Vec<u8>>,
        // TODO cgroup-y info like max memory, cpu time, etc
    },
    Failed,
    NotFound,
    Timeout,
}

/// Locate supported shells on the system.
#[derive(Serialize, Deserialize)]
pub struct ShellListRequest;

/// Start a new shell session
#[derive(Serialize, Deserialize)]
pub struct ShellSessionRequest {
    /// Path to the shell executable
    pub path: PathBuf,

    // TODO request permissions
    // Permission permission = 3;
    /// Additional environment variables
    pub environment: HashMap<String, String>,

    /// Number of rows to request
    pub rows: u32,

    /// Number of columns to request
    pub cols: u32,
}

#[derive(Serialize, Deserialize)]
enum ShellSessionResponse {
    Ok(u64),
}

/// Send standard-input or resizes to a shell session.
#[derive(Serialize, Deserialize, Default)]
pub struct ShellSessionInputEvent {
    /// STDIN data
    pub stdin: Option<Vec<u8>>,

    /// Update the number of rows
    pub rows: Option<u32>,

    /// Update the number of columns
    pub cols: Option<u32>,
}

/// Event containing standard-output and standard-error.
#[derive(Serialize, Deserialize, Default, StreamEvent)]
pub struct ShellSessionOutputEvent {
    pub stdout: Vec<u8>,
    pub stderr: Vec<u8>,
}
