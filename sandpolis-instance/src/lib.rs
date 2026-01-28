use crate::database::{DatabaseLayer, Resident};
use crate::realm::RealmName;
use anyhow::Result;
use native_db::ToKey;
use native_model::Model;
use sandpolis_macros::data;
use serde::{Deserialize, Serialize, de::DeserializeOwned};
use std::cmp::Ordering;
use std::fmt::{Display, Write};
use std::str::FromStr;
use strum::{EnumIter, IntoEnumIterator};
use tracing::trace;
use uuid::Uuid;

pub mod cli;
pub mod config;
pub mod database;
pub mod network;
pub mod realm;

/// Format a UUID in the usual "lowercase hex encoded with hyphens" style.
#[inline]
fn format_uuid(src: u128) -> [u8; 36] {
    let src: [u8; 16] = src.to_le_bytes();
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

/// Shared ID across the entire cluster. This never changes throughout the
/// cluster's lifetime.
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

impl FromStr for ClusterId {
    type Err = anyhow::Error;

    fn from_str(s: &str) -> Result<Self> {
        let uuid = Uuid::parse_str(s)?;
        Ok(ClusterId(uuid.to_u128_le()))
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
#[cfg_attr(feature = "client-gui", derive(bevy::prelude::Component))]
#[derive(Serialize, Deserialize, Clone, Copy, Debug, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct InstanceId(u128);

impl InstanceId {
    /// Generate a new instance ID for an instance of the given type(s).
    pub fn new(instance_types: &[InstanceType]) -> Self {
        if instance_types.is_empty() {
            panic!("No instance type given");
        }

        // Reverse the UUID to make visual comparison easier (dispersing the prefixes
        // makes it easier to tell two UUIDs apart).
        let mut uuid = Uuid::now_v7().to_u128_le();

        // Wipe out last nibble
        uuid &= 0xFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF0;

        for instance_type in instance_types {
            uuid |= instance_type.mask() as u128;
        }

        trace!(id = uuid, "Generated new instance ID");
        Self(uuid)
    }

    /// Generate a new server-only ID as a convenience.
    pub fn new_server() -> Self {
        Self::new(&[InstanceType::Server])
    }

    /// Check whether this UUID was generated with the given instance type.
    pub fn is_type(&self, instance_type: InstanceType) -> bool {
        self.0 & instance_type.mask() as u128 > 0
    }

    pub fn is_server(&self) -> bool {
        self.is_type(InstanceType::Server)
    }

    pub fn is_client(&self) -> bool {
        self.is_type(InstanceType::Client)
    }

    pub fn is_agent(&self) -> bool {
        self.is_type(InstanceType::Agent)
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

impl Default for InstanceId {
    fn default() -> Self {
        Self::new(&[
            #[cfg(feature = "server")]
            InstanceType::Server,
            #[cfg(feature = "client")]
            InstanceType::Client,
            #[cfg(feature = "agent")]
            InstanceType::Agent,
        ])
    }
}

impl Display for InstanceId {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(std::str::from_utf8(&format_uuid(self.0)).unwrap())
    }
}

impl ToKey for InstanceId {
    fn to_key(&self) -> native_db::Key {
        native_db::Key::new(self.0.to_be_bytes().to_vec())
    }

    fn key_names() -> Vec<String> {
        vec!["InstanceId".to_string()]
    }
}

/// All possible instance types.
#[derive(
    Serialize, Deserialize, Clone, Copy, EnumIter, Debug, PartialEq, Eq, PartialOrd, Ord, Hash,
)]
pub enum InstanceType {
    /// Runs continuously on hosts and responds to management requests from
    /// servers and clients.
    Agent,

    /// User interface for managing instances in the Sandpolis network.
    Client,

    /// Runs continously and coordinates interactions among instances in a
    /// Sandpolis network. All networks include at least one server
    /// instance.
    Server,
    // Probes are not technically instances, but they can connect to proper
    // agents/server instances and appear like standalone instances.
}

impl InstanceType {
    /// Each instance type has an identifier.
    pub const fn mask(&self) -> u8 {
        match self {
            InstanceType::Agent => 0b00000001,
            InstanceType::Client => 0b00000010,
            InstanceType::Server => 0b00000100,
        }
    }

    /// Each instance type has an associated color.
    pub const fn color(&self) -> colored::Color {
        match self {
            InstanceType::Agent => colored::Color::Cyan,
            InstanceType::Client => colored::Color::Green,
            InstanceType::Server => colored::Color::Magenta,
        }
    }
}

#[cfg(test)]
mod test_instance_id {
    use super::*;

    #[test]
    fn test_new_single_types() {
        let agent_id = InstanceId::new(&[InstanceType::Agent]);
        assert!(agent_id.is_agent());
        assert!(!agent_id.is_server());
        assert!(!agent_id.is_client());

        let server_id = InstanceId::new(&[InstanceType::Server]);
        assert!(!server_id.is_agent());
        assert!(server_id.is_server());
        assert!(!server_id.is_client());

        let client_id = InstanceId::new(&[InstanceType::Client]);
        assert!(!client_id.is_agent());
        assert!(!client_id.is_server());
        assert!(client_id.is_client());
    }

    #[test]
    fn test_new_multiple_types() {
        let multi_id = InstanceId::new(&[InstanceType::Agent, InstanceType::Server]);
        assert!(multi_id.is_agent());
        assert!(multi_id.is_server());
        assert!(!multi_id.is_client());

        let all_types_id = InstanceId::new(&[
            InstanceType::Agent,
            InstanceType::Server,
            InstanceType::Client,
        ]);
        assert!(all_types_id.is_agent());
        assert!(all_types_id.is_server());
        assert!(all_types_id.is_client());
    }

    #[test]
    fn test_new_server_convenience() {
        let server_id = InstanceId::new_server();
        assert!(server_id.is_server());
        assert!(!server_id.is_agent());
        assert!(!server_id.is_client());
    }

    #[test]
    fn test_types_method() {
        let agent_id = InstanceId::new(&[InstanceType::Agent]);
        let types = agent_id.types();
        assert_eq!(types.len(), 1);
        assert!(types.contains(&InstanceType::Agent));

        let multi_id = InstanceId::new(&[InstanceType::Agent, InstanceType::Server]);
        let types = multi_id.types();
        assert_eq!(types.len(), 2);
        assert!(types.contains(&InstanceType::Agent));
        assert!(types.contains(&InstanceType::Server));
        assert!(!types.contains(&InstanceType::Client));
    }

    #[test]
    fn test_display_format() {
        let id = InstanceId(0x123456789ABCDEF0FEDCBA0987654321);
        let display_str = id.to_string();

        // Should be 36 characters (32 hex + 4 hyphens)
        assert_eq!(display_str.len(), 36);

        // Should contain hyphens in the right positions
        assert_eq!(display_str.chars().nth(8), Some('-'));
        assert_eq!(display_str.chars().nth(13), Some('-'));
        assert_eq!(display_str.chars().nth(18), Some('-'));
        assert_eq!(display_str.chars().nth(23), Some('-'));

        // Should be all lowercase hex
        assert!(
            display_str
                .chars()
                .all(|c| c.is_ascii_hexdigit() || c == '-')
        );
        assert!(display_str.chars().all(|c| !c.is_ascii_uppercase()));
    }

    #[test]
    fn test_type_masks() {
        // Test that the type masks work correctly
        let agent_id = InstanceId::new(&[InstanceType::Agent]);
        let server_id = InstanceId::new(&[InstanceType::Server]);
        let client_id = InstanceId::new(&[InstanceType::Client]);

        // Check that the masks are applied correctly
        assert_eq!(
            agent_id.0 & InstanceType::Agent.mask() as u128,
            InstanceType::Agent.mask() as u128
        );
        assert_eq!(
            server_id.0 & InstanceType::Server.mask() as u128,
            InstanceType::Server.mask() as u128
        );
        assert_eq!(
            client_id.0 & InstanceType::Client.mask() as u128,
            InstanceType::Client.mask() as u128
        );

        // Check that other masks are not set
        assert_eq!(agent_id.0 & InstanceType::Server.mask() as u128, 0);
        assert_eq!(agent_id.0 & InstanceType::Client.mask() as u128, 0);
    }

    #[test]
    fn test_multiple_type_masks() {
        let multi_id = InstanceId::new(&[InstanceType::Agent, InstanceType::Server]);

        // Should have both Agent and Server bits set
        assert_eq!(
            multi_id.0 & InstanceType::Agent.mask() as u128,
            InstanceType::Agent.mask() as u128
        );
        assert_eq!(
            multi_id.0 & InstanceType::Server.mask() as u128,
            InstanceType::Server.mask() as u128
        );

        // Should not have Client bit set
        assert_eq!(multi_id.0 & InstanceType::Client.mask() as u128, 0);
    }

    #[test]
    fn test_mask_bitwise_operations() {
        // Test that the masks are properly distinct
        assert_ne!(InstanceType::Agent.mask(), InstanceType::Server.mask());
        assert_ne!(InstanceType::Agent.mask(), InstanceType::Client.mask());
        assert_ne!(InstanceType::Server.mask(), InstanceType::Client.mask());

        // Test that combining masks works
        let combined = InstanceType::Agent.mask() | InstanceType::Server.mask();
        assert_ne!(combined, InstanceType::Agent.mask());
        assert_ne!(combined, InstanceType::Server.mask());
        assert_eq!(
            combined & InstanceType::Agent.mask(),
            InstanceType::Agent.mask()
        );
        assert_eq!(
            combined & InstanceType::Server.mask(),
            InstanceType::Server.mask()
        );
    }

    #[test]
    #[should_panic(expected = "No instance type given")]
    fn test_new_empty_types_panics() {
        InstanceId::new(&[]);
    }
}

/// A layer represents a functional area of Sandpolis (e.g., filesystem, shell, network).
/// Layers are registered at runtime using the `inventory` crate.
#[cfg_attr(feature = "client-gui", derive(bevy::prelude::Resource))]
#[derive(Debug, Clone, PartialEq, Eq, Hash, PartialOrd, Ord, Serialize, Deserialize)]
pub struct LayerName(pub String);

impl LayerName {
    /// Create a new layer with the given name.
    pub const fn new(name: String) -> Self {
        Self(name)
    }

    /// Get the layer name.
    pub fn name(&self) -> &str {
        &self.0
    }

    /// Iterate over all registered layers.
    pub fn registered() -> impl Iterator<Item = &'static LayerName> {
        inventory::iter::<LayerName>()
    }
}

impl std::fmt::Display for LayerName {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(&self.0)
    }
}

impl std::ops::Deref for LayerName {
    type Target = str;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl From<&str> for LayerName {
    fn from(s: &str) -> Self {
        Self(s.to_string())
    }
}

impl From<String> for LayerName {
    fn from(s: String) -> Self {
        Self(s)
    }
}

impl PartialEq<str> for LayerName {
    fn eq(&self, other: &str) -> bool {
        self.0 == other
    }
}

impl PartialEq<&str> for LayerName {
    fn eq(&self, other: &&str) -> bool {
        self.0 == *other
    }
}

// Register Layer with inventory for runtime collection
inventory::collect!(LayerName);

#[data]
#[derive(Default)]
pub struct InstanceLayerData {
    pub cluster_id: ClusterId,
    pub instance_id: InstanceId,
    pub os_info: os_info::Info,
}

#[derive(Clone)]
#[cfg_attr(feature = "client-gui", derive(bevy::prelude::Resource))]
pub struct InstanceLayer {
    data: Resident<InstanceLayerData>,
    pub instance_id: InstanceId,
    pub cluster_id: ClusterId,
}

impl InstanceLayer {
    pub async fn new(database: DatabaseLayer) -> Result<Self> {
        let data: Resident<InstanceLayerData> =
            database.realm(RealmName::default())?.resident(())?;

        Ok(Self {
            instance_id: { data.read().instance_id },
            cluster_id: { data.read().cluster_id },
            data,
        })
    }
}

#[derive(Serialize, Deserialize, Clone, Debug, PartialEq, Eq)]
pub struct LayerVersion {
    pub major: u32,
    pub minor: u32,
    pub patch: u32,
    pub description: Option<String>,
    // pub rev: Option<String>,
    // pub build_time: Option<String>,
}

impl TryFrom<String> for LayerVersion {
    type Error = anyhow::Error;

    fn try_from(value: String) -> Result<Self, Self::Error> {
        todo!()
    }
}

impl PartialOrd for LayerVersion {
    fn partial_cmp(&self, other: &Self) -> Option<Ordering> {
        Some(self.cmp(other))
    }
}

impl Ord for LayerVersion {
    fn cmp(&self, other: &Self) -> Ordering {
        if self.major > other.major {
            Ordering::Greater
        } else if self.major < other.major {
            Ordering::Less
        } else if self.minor > other.minor {
            Ordering::Greater
        } else if self.minor < other.minor {
            Ordering::Less
        } else if self.patch > other.patch {
            Ordering::Greater
        } else if self.patch < other.patch {
            Ordering::Less
        } else {
            Ordering::Equal
        }
    }
}

/// A config fragment that can take overrides from the command line or from
/// environment variables.
pub trait LayerConfig<C>
where
    C: clap::Parser,
    Self: Serialize + DeserializeOwned,
{
    /// Override the config with values from the command line
    fn override_cli(&mut self, args: &C) {
        // Default no-op
    }

    /// Override the config with values from the environment
    fn override_env(&mut self) {
        // Default no-op
    }
}
