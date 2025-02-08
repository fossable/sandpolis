//! Instance layer

use serde::{Deserialize, Serialize};
use std::{cmp::Ordering, fmt::Display, ops::Deref, path::PathBuf, str::FromStr};
use strum::{EnumIter, IntoEnumIterator};
use uuid::Uuid;

pub mod cli;

/// Format a UUID in the usual "lowercase hex encoded with hyphens" style.
#[inline]
fn format_uuid(src: u128) -> [u8; 36] {
    let src: [u8; 16] = src.to_be_bytes();
    let lut = [
        b'0', b'1', b'2', b'3', b'4', b'5', b'6', b'7', b'8', b'9', b'a', b'b', b'c', b'd', b'e',
        b'f',
    ];

    let groups = [(0, 8), (9, 13), (14, 18), (19, 23), (24, 36)];
    let mut dst = [0; 36];

    let mut group_idx = 0;
    let mut i = 0;
    while group_idx < 5 {
        let (start, end) = groups[group_idx];
        let mut j = start;
        while j < end {
            let x = src[i];
            i += 1;

            dst[j] = lut[(x >> 4) as usize];
            dst[j + 1] = lut[(x & 0x0f) as usize];
            j += 2;
        }
        if group_idx < 4 {
            dst[end] = b'-';
        }
        group_idx += 1;
    }
    dst
}

/// Shared ID across the entire cluster. This never changes throughout the cluster's
/// lifetime.
#[derive(Serialize, Deserialize, Clone, Copy, Debug, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct ClusterId(u128);

impl ClusterId {
    pub fn as_bytes(&self) -> [u8; 16] {
        self.0.to_be_bytes()
    }
}

impl Display for ClusterId {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(std::str::from_utf8(&format_uuid(self.0)).unwrap())
    }
}

impl Default for ClusterId {
    fn default() -> Self {
        Self(Uuid::now_v7().to_u128_le())
    }
}

#[cfg(test)]
mod test_cluster_id {
    use super::*;
    #[test]
    fn test_display() {
        assert_eq!(ClusterId::default().to_string().len(), 36);
    }
}

/// All instances are identified by a unique 128-bit string that's generated on
/// first start. This identifier is reused for all subsequent runs.
#[derive(Serialize, Deserialize, Clone, Copy, Debug, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct InstanceId(u128);

impl InstanceId {
    /// Generate a new instance ID for an instance of the given type(s).
    pub fn new(instance_types: &[InstanceType]) -> Self {
        // Reverse the UUID to make visual comparison easier (dispersing the prefixes
        // makes it easier to tell two UUIDs apart).
        let mut uuid = Uuid::now_v7().to_u128_le();

        // Wipe out last nibble
        uuid &= 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF0;

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
        f.write_str(std::str::from_utf8(&format_uuid(self.0)).unwrap())
    }
}

/// All possible instance types.
#[derive(
    Serialize, Deserialize, Clone, Copy, EnumIter, Debug, PartialEq, Eq, PartialOrd, Ord, Hash,
)]
pub enum InstanceType {
    /// Runs continuously on hosts and responds to management requests from servers
    /// and clients.
    Agent,

    /// User interface for managing instances in the Sandpolis network.
    Client,

    /// Runs continously and coordinates interactions among instances in a Sandpolis
    /// network. All networks include at least one server instance.
    Server,
}

impl InstanceType {
    /// Each instance type has an identifier.
    const fn mask(&self) -> u8 {
        match self {
            InstanceType::Agent => 0b00000001,
            InstanceType::Client => 0b00000010,
            InstanceType::Server => 0b00000100,
        }
    }
}

#[cfg(test)]
mod test_instance_id {
    use super::*;
    #[test]
    fn test_display() {
        assert_eq!(
            InstanceId::new(&[InstanceType::Agent]).to_string().len(),
            36
        );
    }

    #[test]
    fn test_types() {
        assert!(InstanceId::new(&[
            InstanceType::Agent,
            InstanceType::Server,
            InstanceType::Client
        ])
        .is_type(InstanceType::Agent));
        assert!(InstanceId::new(&[InstanceType::Agent]).is_type(InstanceType::Agent));
        assert!(!InstanceId::new(&[InstanceType::Server]).is_type(InstanceType::Agent));
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

/// Layers are feature-sets that may be enabled on instances.
#[derive(Serialize, Deserialize, Clone, Copy, EnumIter, Debug, PartialEq, Eq, Hash)]
pub enum Layer {
    /// Manage accounts.
    #[cfg(feature = "layer-account")]
    Account,

    Agent,

    #[cfg(feature = "layer-alert")]
    Alert,

    Client,

    /// Deploy agents directly over a protocol like SSH or via special deployer packages.
    #[cfg(feature = "layer-deploy")]
    Deploy,

    /// Interact with Desktop environments.
    #[cfg(feature = "layer-desktop")]
    Desktop,
    /// Mount and manipulate filesystems.
    #[cfg(feature = "layer-filesystem")]
    Filesystem,

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

    /// Support for connecting to instances in the Sandpolis network and sending
    /// messages back and forth.
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

    #[cfg(feature = "layer-snapshot")]
    Snapshot,

    #[cfg(feature = "layer-sysinfo")]
    Sysinfo,

    /// Establish persistent or ephemeral tunnels between instances.
    #[cfg(feature = "layer-tunnel")]
    Tunnel,
}

#[derive(Serialize, Deserialize, Clone, Debug, PartialEq, Eq, Ord)]
pub struct LayerVersion {
    major: u32,
    minor: u32,
    patch: u32,
}

impl PartialOrd for LayerVersion {
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        if self.major > other.major {
            Some(Ordering::Greater)
        } else if self.major < other.major {
            Some(Ordering::Less)
        } else if self.minor > other.minor {
            Some(Ordering::Greater)
        } else if self.minor < other.minor {
            Some(Ordering::Less)
        } else if self.patch > other.patch {
            Some(Ordering::Greater)
        } else if self.patch < other.patch {
            Some(Ordering::Less)
        } else {
            Some(Ordering::Equal)
        }
    }
}
