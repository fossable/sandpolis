use serde::{Deserialize, Serialize};
use std::ops::Deref;
use strum::EnumIter;

pub mod database;
pub mod layer;
pub mod random;

/// Major system architecture types.
#[derive(Serialize, Deserialize, Clone, Copy, EnumIter, Debug)]
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
#[derive(Serialize, Deserialize, Clone, Copy, EnumIter, Debug)]
pub enum InstanceType {
    /// A headless application that provides read/write access to a host
    Agent,

    /// A UEFI agent that runs in a pre-boot environment
    BootAgent,

    /// A UI application used for managing agents and servers
    Client,

    /// A headless application that installs or updates an agent or probe
    Deployer,

    /// A headless application that coordinates interaction among instances
    Server,
}

impl InstanceType {
    const fn mask(&self) -> u8 {
        match self {
            InstanceType::Agent => 0b00000001,
            InstanceType::BootAgent => 0b00000010,
            InstanceType::Client => 0b00000100,
            InstanceType::Deployer => 0b00001000,
            InstanceType::Server => 0b00100000,
        }
    }
}

#[derive(Serialize, Deserialize, Clone, Copy, Debug, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct InstanceId(uuid::Uuid);

impl Deref for InstanceId {
    type Target = uuid::Uuid;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl InstanceId {
    pub fn new(instance_types: &[InstanceType]) -> Self {
        let mut bytes = uuid::Uuid::now_v7().as_bytes().to_owned();

        bytes[15] = 0;
        for instance_type in instance_types {
            bytes[15] |= instance_type.mask();
        }
        Self(uuid::Uuid::from_bytes(bytes))
    }

    pub fn check(&self, instance_type: InstanceType) -> bool {
        self.as_bytes()[15] & instance_type.mask() > 0
    }
}

/// Layers are optional feature-sets that may be enabled on instances.
#[derive(Serialize, Deserialize, Clone, Copy, EnumIter, Debug, PartialEq, Eq)]
pub enum Layer {
    /// Manage accounts.
    #[cfg(feature = "layer-account")]
    Account,

    #[cfg(feature = "layer-alerts")]
    Alerts,

    /// Interact with Desktop environments.
    #[cfg(feature = "layer-desktop")]
    Desktop,

    // Docker,
    /// Mount and manipulate filesystems.
    #[cfg(feature = "layer-filesystem")]
    Filesystem,

    // Goldboot,
    #[cfg(feature = "layer-health")]
    Health,

    /// View system information.
    #[cfg(feature = "layer-inventory")]
    Inventory,

    // Libvirt,
    Location,

    /// View logs.
    #[cfg(feature = "layer-logging")]
    Logging,

    Meta,
    Network,
    #[cfg(feature = "layer-packages")]
    Packages,

    /// Support for probe devices which do not run agent software. Instead they
    /// connect through a "gateway" instance over a well known protocol.
    #[cfg(feature = "layer-probe")]
    Probe,

    /// Interact with shell prompts / snippets.
    #[cfg(feature = "layer-shell")]
    Shell,
    // Tunnel,
}
