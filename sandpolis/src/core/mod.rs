use serde::{Deserialize, Serialize};
use std::ops::Deref;
use strum::EnumIter;

pub mod database;
pub mod random;

/// Major system architecture types.
#[derive(Serialize, Deserialize, Clone, Copy, EnumIter)]
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
#[derive(Serialize, Deserialize, Clone, Copy, EnumIter)]
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

impl InstanceType {
    const fn mask(&self) -> u8 {
        match self {
            InstanceType::Agent => 0b00000001,
            InstanceType::BootAgent => 0b00000010,
            InstanceType::Client => 0b00000100,
            InstanceType::Deployer => 0b00001000,
            InstanceType::Probe => 0b00010000,
            InstanceType::Server => 0b00100000,
        }
    }
}

#[derive(Serialize, Deserialize, Clone, Copy)]
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

#[derive(Serialize, Deserialize, Clone, Copy, EnumIter)]
pub enum Layer {
    #[cfg(feature = "layer-desktop")]
    Desktop,
}
