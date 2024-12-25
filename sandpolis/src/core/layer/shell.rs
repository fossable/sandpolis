
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

/// Request to execute a command in a shell.
pub struct ShellExecuteRequest {

    /// Shell executable to use for request
    pub shell: PathBuf,

    /// Command to execute in a new shell
    pub command: Vec<String>,

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
    pub type: ShellType,

    /// Location of the shell executable
    pub location: PathBuf,

    // Version number if available
    pub version: Option<String>,
}

/// Supported shell information.
pub struct ShellListResponse {
    pub shells: Vec<DiscoveredShell>,
}

// Request to start a new shell session
message RQ_ShellStream {

    // The desired stream ID
    int32 stream_id = 1;

    // The path to the shell executable
    string path = 2;

    // TODO request permissions
    // Permission permission = 3;

    // Additional environment variables
    map<string, string> environment = 4;

    // The number of rows to request
    int32 rows = 5;

    // The number of columns to request
    int32 cols = 6;
}

enum RS_ShellStream {
    SHELL_STREAM_OK = 0;
}

/// Send standard-input or resizes to a shell session.
pub struct ShellStreamInputEvent {

    /// STDIN data
    pub stdin: Option<Vec<u8>>,

    /// Update the number of rows
    pub rows: Option<u32>,

    /// Update the number of columns
    pub cols: Option<u32>,
}

// Event containing standard-output and standard-error
message EV_ShellStreamOutput {

    // The process standard-output
    bytes stdout = 1;

    // The process standard-error
    bytes stderr = 2;
}
