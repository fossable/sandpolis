use anyhow::Result;
use sandpolis_macros::{Delta, StreamEvent};
use serde::{Deserialize, Serialize};
use std::{collections::HashMap, path::PathBuf};

#[cfg(feature = "agent")]
pub mod agent;
pub mod messages;

/// Build info
pub mod built_info {
    include!(concat!(env!("OUT_DIR"), "/built.rs"));
}

#[derive(Clone)]
pub struct ShellLayer {}

impl ShellLayer {
    pub fn new() -> Result<Self> {
        Ok(Self {})
    }
}

#[derive(Serialize, Deserialize, Clone)]
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

#[derive(Serialize, Deserialize, Clone, Delta)]
pub struct ShellSessionData {
    pub shell_type: ShellType,

    /// Path to the shell's executable
    pub shell: PathBuf,

    pub started: Option<u64>,

    pub ended: Option<u64>,

    /// Total CPU time used by the process and all children
    pub cpu_time: f64,

    /// Peak memory used by the process and all children
    pub max_memory: u64,
}

/// A reusable shell script.
#[derive(Serialize, Deserialize)]
pub struct ShellSnippet {
    pub shell_type: ShellType,
    pub content: String,
}

#[derive(Serialize, Deserialize)]
pub enum ShellCommand {
    /// Inline command
    Command(Vec<String>),
    /// Snippet ID
    Snippet(String),
}

#[derive(Serialize, Deserialize)]
pub struct DiscoveredShell {
    /// Closest type of discovered shell
    pub shell_type: ShellType,

    /// Location of the shell executable
    pub location: PathBuf,

    // Version number if available
    pub version: Option<String>,
}

/// Supported shell information.
#[derive(Serialize, Deserialize)]
pub struct ShellListResponse {
    pub shells: Vec<DiscoveredShell>,
}
