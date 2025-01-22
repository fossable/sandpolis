use serde::{Deserialize, Serialize};
use std::{fmt::Display, ops::Deref, path::PathBuf, str::FromStr};
use strum::{EnumIter, IntoEnumIterator};
use uuid::Uuid;

pub mod database;
pub mod format;
pub mod layer;
pub mod random;

/// All instances are identified by a unique 128-bit string that's generated on
/// first start. This identifier is reused for all subsequent runs.
#[derive(Serialize, Deserialize, Clone, Copy, Debug, PartialEq, Eq, PartialOrd, Ord, Hash)]
#[cfg_attr(feature = "client", derive(bevy::prelude::Component))]
pub struct InstanceId(u128);

impl InstanceId {
    /// Generate a new instance ID for an instance of the given type(s).
    pub fn new(instance_types: &[InstanceType]) -> Self {
        // Reverse the UUID to make visual comparison easier (dispersing the prefixes
        // makes it easier to tell two UUIDs apart).
        let mut uuid = Uuid::now_v7().to_u128_le();

        // Wipe out last byte
        uuid &= 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00;

        for instance_type in instance_types {
            uuid |= instance_type.mask() as u128;
        }
        Self(uuid)
    }

    /// Check whether this UUID was generated with the given instance type.
    pub fn is_type(&self, instance_type: InstanceType) -> bool {
        self.0 & instance_type.mask() as u128 > 0
    }

    /// Return the instance types encoded in this UUID.
    pub fn types(&self) -> Vec<InstanceType> {
        InstanceType::iter().filter(|t| self.is_type(*t)).collect()
    }

    /// Return the timestamp when the UUID was generated.
    pub fn timestamp(&self) -> u64 {
        todo!()
    }
}

impl Display for InstanceId {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(std::str::from_utf8(&format::uuid(self.0)).unwrap())
    }
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
    /// Each instance type has an identifier.
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

/// Shared ID across the entire cluster. This never changes throughout the cluster's
/// lifetime.
#[derive(Serialize, Deserialize, Clone, Copy, Debug, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct ClusterId(u128);

impl Display for ClusterId {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(std::str::from_utf8(&format::uuid(self.0)).unwrap())
    }
}

impl Default for ClusterId {
    fn default() -> Self {
        Self(Uuid::now_v7().to_u128_le())
    }
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
