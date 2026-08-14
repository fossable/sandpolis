//! Where the agent binary a deployment installs comes from.
//!
//! Nothing produces prebuilt agent binaries yet, so the default source refuses
//! every request. The rest of the deployment is written around this trait
//! rather than around a concrete source, so publishing binaries later is a
//! matter of calling [`install_binary_source`] at startup — the SSH path
//! doesn't change.

use crate::deploy::os::TargetOs;
use anyhow::{Result, bail};
use std::sync::OnceLock;

/// Identifies the binary a particular target needs.
#[derive(Clone, Debug)]
pub struct AgentBinaryKey {
    pub os: TargetOs,
    /// Machine architecture as `uname -m` reports it (e.g. `x86_64`).
    pub arch: String,
}

impl std::fmt::Display for AgentBinaryKey {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}/{}", self.os, self.arch)
    }
}

/// Supplies agent binaries to install on deploy targets.
pub trait AgentBinarySource: Send + Sync + 'static {
    /// The complete agent executable for `key`.
    fn resolve(&self, key: &AgentBinaryKey) -> Result<Vec<u8>>;
}

static SOURCE: OnceLock<Box<dyn AgentBinarySource>> = OnceLock::new();

/// Install the process-wide binary source. The first caller wins.
pub fn install_binary_source(source: impl AgentBinarySource) {
    let _ = SOURCE.set(Box::new(source));
}

/// The agent binary for `key`, or an error explaining that none is available.
pub fn resolve(key: &AgentBinaryKey) -> Result<Vec<u8>> {
    match SOURCE.get() {
        Some(source) => source.resolve(key),
        None => bail!(
            "no prebuilt agent binary available for {key}. Install an agent on \
             the target manually and deploy again to configure it."
        ),
    }
}
