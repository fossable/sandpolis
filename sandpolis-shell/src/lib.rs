use anyhow::Result;
use native_db::*;
use native_model::Model;
use sandpolis_core::InstanceId;
use sandpolis_database::DatabaseLayer;
use sandpolis_macros::{Stream, data};
use sandpolis_network::{RegisterResponders, StreamRegistry, StreamRequester};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::path::PathBuf;
use std::sync::Arc;
use tokio::sync::mpsc::Sender;
use tokio::sync::{Mutex, RwLock};

use crate::execute::{ShellExecuteStreamRequest, ShellExecuteStreamResponse};

#[cfg(feature = "client")]
pub mod client;
pub mod execute;
pub mod session;
pub mod shell;

/// Build info
pub mod built_info {
    include!(concat!(env!("OUT_DIR"), "/built.rs"));
}

#[derive(Clone)]
pub struct ShellLayer {
    database: DatabaseLayer,
    #[cfg(feature = "agent")]
    sessions: Arc<Mutex<Vec<crate::session::ShellSessionStreamResponder>>>,
}

impl ShellLayer {
    pub async fn new(database: DatabaseLayer) -> Result<Self> {
        Ok(Self {
            database,
            #[cfg(feature = "agent")]
            sessions: Arc::new(Mutex::new(Vec::new())),
        })
    }
}

/// Static handler for registering shell stream responders.
#[cfg(feature = "agent")]
pub struct ShellResponderRegistration;

#[cfg(feature = "agent")]
impl RegisterResponders for ShellResponderRegistration {
    fn register_responders(&self, registry: &StreamRegistry) {
        registry.register_responder(|| execute::ShellExecuteStreamResponder);
        registry.register_responder(|| session::ShellSessionStreamResponder {
            process: tokio::sync::RwLock::new(None),
            stdin: tokio::sync::RwLock::new(None),
        });
    }
}

#[cfg(feature = "agent")]
inventory::submit!(sandpolis_network::ResponderRegistration(
    &ShellResponderRegistration
));

#[derive(Serialize, Deserialize, Clone, PartialEq, Eq, Debug, Default)]
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
    /// Fish shell (https://fishshell.com)
    Fish,
    /// Korn shell (http://kornshell.com)
    Ksh,
    Powershell,
    /// Generic POSIX shell
    Sh,

    /// We couldn't figure out the shell type
    #[default]
    Unknown,

    /// Z shell
    Zsh,
}

#[data]
pub struct ShellSessionData {
    #[secondary_key]
    pub _instance_id: InstanceId,

    pub shell_type: ShellType,

    /// Path to the shell's executable
    pub shell: Option<PathBuf>,

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

#[derive(Stream)]
pub struct ShellExecuteStreamRequester {
    exit_code: RwLock<Option<i32>>,
    duration: RwLock<Option<f64>>,
    output: HashMap<i32, Vec<u8>>,
}

impl StreamRequester for ShellExecuteStreamRequester {
    type In = ShellExecuteStreamResponse;
    type Out = ShellExecuteStreamRequest;

    async fn on_message(&self, response: Self::In, _: Sender<Self::Out>) -> Result<()> {
        match response {
            ShellExecuteStreamResponse::Done {
                exit_code,
                duration,
            } => {
                *self.exit_code.write().await = Some(exit_code);
            }
            ShellExecuteStreamResponse::Progress { output } => todo!(),
            ShellExecuteStreamResponse::Failed => todo!(),
            ShellExecuteStreamResponse::NotFound => todo!(),
            ShellExecuteStreamResponse::Timeout => todo!(),
        }
        Ok(())
    }

    async fn new(initial: Self::Out, tx: Sender<Self::Out>) -> Result<Self> {
        tx.send(initial).await?;
        Ok(Self {
            exit_code: RwLock::new(None),
            duration: RwLock::new(None),
            output: HashMap::new(),
        })
    }
}
