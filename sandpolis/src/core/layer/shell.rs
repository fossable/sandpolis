use std::{collections::HashMap, path::PathBuf};

pub struct ShellLayer {
    tree: sled::Tree,
}

pub enum ShellType {
    /// Busybox shell
    Ash,

    /// Bourne Again shell
    Bash,

    /// Microsoft CMD.EXE
    CmdExe,
    /// C shell
    Csh,
    Dash,
    /// Fish shell
    Fish,
    /// Korn shell
    Ksh,
    Powershell,
    /// Generic POSIX shell
    Sh,
    /// Z shell
    Zsh,
}

pub struct ShellSessionData {
    pub shell_type: ShellType,

    /// Path to the shell's executable
    pub shell: PathBuf,

    /// Total CPU time used by the process and all children
    pub cpu_time: f64,

    /// Peak memory used by the process and all children
    pub max_memory: u64,
}

/// A reusable shell script.
pub struct ShellSnippet {
    pub shell_type: ShellType,
    pub content: String,
}

pub enum ShellCommand {
    /// Inline command
    Command(Vec<String>),
    /// Snippet ID
    Snippet(String),
}

/// Register a scheduled command to execute in a shell.
pub struct ShellScheduleRequest {
    /// Shell executable to use for request
    pub shell: PathBuf,

    /// Command to execute in a new shell
    pub command: ShellCommand,

    /// Execution timeout in seconds
    pub timeout: u64,
}

/// Request to execute a command in a shell.
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
pub struct ShellExecuteResponse {
    /// Process exit code
    pub exit_code: i32,

    /// Execution duration in seconds
    pub duration: f64,

    /// Process output on all descriptors
    pub output: HashMap<i32, Vec<u8>>,
    // TODO cgroup-y info like max memory, cpu time, etc
}

/// Locate supported shells on the system.
pub struct ShellListRequest;

pub struct DiscoveredShell {
    /// Closest type of discovered shell
    pub shell_type: ShellType,

    /// Location of the shell executable
    pub location: PathBuf,

    // Version number if available
    pub version: Option<String>,
}

/// Supported shell information.
pub struct ShellListResponse {
    pub shells: Vec<DiscoveredShell>,
}

// Start a new shell session
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

enum ShellSessionResponse {
    Ok(u64),
}

/// Send standard-input or resizes to a shell session.
pub struct ShellSessionInputEvent {
    /// STDIN data
    pub stdin: Option<Vec<u8>>,

    /// Update the number of rows
    pub rows: Option<u32>,

    /// Update the number of columns
    pub cols: Option<u32>,
}

/// Event containing standard-output and standard-error
pub struct ShellSessionOutputEvent {
    pub output: HashMap<i32, Vec<u8>>,
}
