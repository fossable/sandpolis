use serde::{Deserialize, Serialize};
use std::{fmt::Display, ops::Deref, path::PathBuf, str::FromStr};
use strum::EnumIter;

pub mod database;
pub mod layer;
pub mod random;

/// The official server port: https://www.iana.org/assignments/service-names-port-numbers/service-names-port-numbers.xhtml?search=8768
pub const S7S_PORT: u16 = 8768;

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

    /// A headless application that installs or updates an agent
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

impl From<InstanceId> for InstanceType {
    fn from(value: InstanceId) -> Self {
        todo!()
    }
}

#[derive(Serialize, Deserialize, Clone, Copy, Debug, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct ClusterId(uuid::Uuid);

impl Deref for ClusterId {
    type Target = uuid::Uuid;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl AsRef<[u8]> for ClusterId {
    fn as_ref(&self) -> &[u8] {
        self.0.as_bytes()
    }
}

#[derive(Serialize, Deserialize, Clone, Copy, Debug, PartialEq, Eq, PartialOrd, Ord, Hash)]
#[cfg_attr(feature = "client", derive(bevy::prelude::Component))]
pub struct InstanceId(uuid::Uuid);

impl InstanceId {
    /// Generate a new instance ID for an instance of the given type(s).
    pub fn new(instance_types: &[InstanceType]) -> Self {
        // TODO reverse?
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

impl Display for InstanceId {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(&self.0.to_string())
    }
}

impl Deref for InstanceId {
    type Target = uuid::Uuid;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl AsRef<[u8]> for InstanceId {
    fn as_ref(&self) -> &[u8] {
        self.0.as_bytes()
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

    /// Deploy new Sandpolis agents.
    #[cfg(feature = "layer-deploy")]
    Deploy,

    /// Interact with Desktop environments.
    #[cfg(feature = "layer-desktop")]
    Desktop,

    /// Mount and manipulate filesystems.
    #[cfg(feature = "layer-filesystem")]
    Filesystem,

    // Goldboot,
    #[cfg(feature = "layer-health")]
    Health,

    /// View system information.
    #[cfg(feature = "layer-inventory")]
    Inventory,

    #[cfg(feature = "layer-location")]
    Location,

    /// Aggregate and view logs.
    #[cfg(feature = "layer-logging")]
    Logging,

    /// Manage the Sandpolis network.
    Network,

    #[cfg(feature = "layer-package")]
    Package,

    /// Support for probe devices which do not run agent software. Instead they
    /// connect through a "gateway" instance over a well known protocol.
    #[cfg(feature = "layer-probe")]
    Probe,

    Server,

    /// Interact with shell prompts / snippets.
    #[cfg(feature = "layer-shell")]
    Shell,

    /// Establish persistent or ephemeral tunnels between instances.
    #[cfg(feature = "layer-tunnel")]
    Tunnel,

    User,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct InstanceData {
    pub id: InstanceId,
    pub os_info: os_info::Info,
}

impl Default for InstanceData {
    fn default() -> Self {
        Self {
            id: InstanceId::new(&[
                #[cfg(feature = "server")]
                InstanceType::Server,
                #[cfg(feature = "client")]
                InstanceType::Client,
                #[cfg(feature = "agent")]
                InstanceType::Agent,
            ]),
            os_info: os_info::get(),
        }
    }
}
