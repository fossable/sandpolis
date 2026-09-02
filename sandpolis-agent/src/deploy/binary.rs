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

    /// Whether [`resolve`](Self::resolve) would succeed, without keeping the
    /// binary around.
    fn check(&self, key: &AgentBinaryKey) -> Result<()> {
        self.resolve(key).map(|_| ())
    }
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

/// Whether [`resolve`] would succeed for `key`, without materializing the
/// binary.
pub fn check(key: &AgentBinaryKey) -> Result<()> {
    match SOURCE.get() {
        Some(source) => source.check(key),
        None => bail!(
            "no prebuilt agent binary available for {key}. Install an agent on \
             the target manually and deploy again to configure it."
        ),
    }
}

#[cfg(test)]
mod test {
    use super::*;

    /// With no source installed, a dry run's availability check reports the
    /// same "no prebuilt agent binary" blocker a real deployment would hit.
    #[test]
    fn check_without_a_source_names_the_blocker() {
        let key = AgentBinaryKey {
            os: TargetOs::Linux("debian".to_string()),
            arch: "x86_64".to_string(),
        };
        let error = check(&key).expect_err("no source is installed in tests");
        assert!(error.to_string().contains("no prebuilt agent binary"));
    }
}
