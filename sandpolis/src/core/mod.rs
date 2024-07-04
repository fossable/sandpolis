use serde::{Deserialize, Serialize};

pub mod random;

/// Major OS types.
#[derive(Serialize, Deserialize)]
pub enum OsClassification {
    Linux,
    Macos,
    Ios,
    FreeBsd,
    NetBsd,
    OpenBsd,
    Solaris,
    Android,
    Windows,
}

/// Major system architecture types.
#[derive(Serialize, Deserialize)]
pub enum ArchClassification {
    X86,
    X86_64,
    Arm,
    Aarch64,
    Mips,
    Mips64,
    Riscv64,
    S390X,
    Sparc64,
}

/// All possible instance types.
#[derive(Serialize, Deserialize)]
pub enum InstanceType {
    /// A headless application that provides read/write access to a host
    Agent,

    /// A UEFI agent that runs in a pre-boot environment
    BootAgent,

    /// A UI application used for managing agents and servers
    Client,

    /// A headless application that installs or updates an agent or probe
    Deployer,

    /// A headless application that provides read-only access to a host
    Probe,

    /// A headless application that coordinates interaction among instances
    Server,
}
