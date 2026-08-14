//! Agent deployment over SSH.
//!
//! A client hands the server everything needed to reach one host — address,
//! username, and credentials — and the *server* opens the SSH connection. The
//! client never touches SSH, which is why every module here that speaks russh
//! is gated on the `server` feature.
//!
//! Deployment walks a fixed sequence of [`DeployStep`]s and reports each one
//! back over the stream, so the client can show progress rather than a spinner
//! that hides a two-minute upload. A host that already has an agent installed
//! stops after [`DeployStep::Upload`]: rewriting its `.server` file is the whole
//! job, since the agent reads its connection policy from that file.

use serde::{Deserialize, Serialize};

pub mod binary;
#[cfg(feature = "client")]
pub mod client;
pub mod os;
#[cfg(feature = "server")]
pub mod server;
#[cfg(feature = "server")]
pub mod systemd;

/// Where the agent binary is installed on the target.
pub const INSTALL_PATH: &str = "/opt/sandpolis/sandpolis";

/// The agent's data directory on the target, which is also where its `.server`
/// file goes.
pub const DATA_PATH: &str = "/var/lib/sandpolis";

/// The `.server` file written into [`DATA_PATH`].
pub const SERVER_FILE: &str = "/var/lib/sandpolis/default.server";

/// How the server authenticates to the target host.
///
/// Whichever variant is used, the secret is chosen on the client (from the
/// operator's own SSH setup) and travels to the server inside the deploy
/// request. It is never persisted: the server holds it only for the life of the
/// deployment.
#[derive(Serialize, Deserialize, Clone)]
pub enum DeployAuth {
    Password(String),
    /// A private key in PEM form, read from the operator's key file.
    PrivateKey {
        pem: String,
        passphrase: Option<String>,
    },
}

impl std::fmt::Debug for DeployAuth {
    /// Hand-written so a request that gets logged can't spill a key or
    /// password.
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Self::Password(_) => f.write_str("Password(<redacted>)"),
            Self::PrivateKey { .. } => f.write_str("PrivateKey(<redacted>)"),
        }
    }
}

/// The machine that will receive the agent installation.
#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct DeployTarget {
    pub host: String,
    pub port: u16,
    pub username: String,
    pub auth: DeployAuth,
    /// Expected SHA256 host key fingerprint, with or without the `SHA256:`
    /// prefix. Without one the host key is accepted on first use.
    pub fingerprint: Option<String>,
}

/// Requests from a client to the server's deployer.
#[derive(Serialize, Deserialize, Debug)]
pub enum DeployStreamRequest {
    /// Deploy an agent that will connect back to `server`.
    Start {
        target: DeployTarget,
        /// The server the deployed agent will connect to. Its
        /// [`canonical`](sandpolis_instance::realm::url::ServerUrl::canonical)
        /// form becomes the common name of the minted agent certificate, and
        /// its realm decides which CA signs it.
        server: sandpolis_instance::realm::url::ServerUrl,
        /// Run the deployed agent in polling mode instead of keeping it
        /// continuously connected.
        poll: Option<sandpolis_instance::realm::config::PollConfig>,
    },
}

/// One stage of a deployment. The client renders these in order, so they are
/// listed in the order they run.
#[derive(Serialize, Deserialize, Clone, Copy, Debug, PartialEq, Eq)]
pub enum DeployStep {
    /// Open and authenticate the SSH connection.
    Connect,
    /// Identify the target's OS and architecture, and look for an existing
    /// installation.
    Probe,
    /// Mint an agent certificate from the realm CA.
    Certificate,
    /// Write the `.server` file to the target's data directory.
    Upload,
    /// Upload the agent binary and install its systemd unit.
    Service,
    /// Enable and start the service.
    Start,
}

impl DeployStep {
    /// Every step, in the order they run.
    pub const ALL: [DeployStep; 6] = [
        Self::Connect,
        Self::Probe,
        Self::Certificate,
        Self::Upload,
        Self::Service,
        Self::Start,
    ];

    /// Short label for the client's progress list.
    pub const fn label(&self) -> &'static str {
        match self {
            Self::Connect => "Connect",
            Self::Probe => "Probe",
            Self::Certificate => "Certificate",
            Self::Upload => "Upload",
            Self::Service => "Service",
            Self::Start => "Start",
        }
    }
}

/// Responses from the server's deployer.
#[derive(Serialize, Deserialize, Debug)]
pub enum DeployStreamResponse {
    /// A step has started, with a line describing what it's doing.
    Step { step: DeployStep, message: String },
    /// A step finished successfully.
    Done { step: DeployStep },
    /// The deployment finished. `reconfigured` means the target already had an
    /// agent, so only its `.server` file was rewritten.
    Finished { reconfigured: bool },
    /// The deployment failed during `step` and has stopped.
    Failed { step: DeployStep, message: String },
}
