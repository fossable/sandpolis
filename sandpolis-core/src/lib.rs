use anyhow::Result;
use colored::Color;
use native_db::ToKey;
use regex::Regex;
use serde::{Deserialize, Serialize};
use std::fmt::{Display, Write};
use std::ops::Deref;
use std::str::FromStr;
use strum::{EnumIter, IntoEnumIterator};
use uuid::Uuid;
use validator::{Validate, ValidationErrors};

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
        if instance_types.len() == 0 {
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
    pub const fn color(&self) -> Color {
        match self {
            InstanceType::Agent => Color::Cyan,
            InstanceType::Client => Color::Green,
            InstanceType::Server => Color::Magenta,
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
        assert!(
            InstanceId::new(&[
                InstanceType::Agent,
                InstanceType::Server,
                InstanceType::Client
            ])
            .is_type(InstanceType::Agent)
        );
        assert!(InstanceId::new(&[InstanceType::Agent]).is_type(InstanceType::Agent));
        assert!(InstanceId::new(&[InstanceType::Server]).is_type(InstanceType::Server));
        assert!(InstanceId::new(&[InstanceType::Client]).is_type(InstanceType::Client));
        assert!(!InstanceId::new(&[InstanceType::Server]).is_type(InstanceType::Agent));
    }
}

/// Realms have unique names and are shared across the entire cluster. Realm
/// names cannot be changed after they are created.
#[derive(Serialize, Deserialize, Debug, Clone, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub struct RealmName(String);

impl Default for RealmName {
    fn default() -> Self {
        Self("default".into())
    }
}

impl Display for RealmName {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(&self.0)
    }
}

impl Deref for RealmName {
    type Target = String;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl FromStr for RealmName {
    type Err = anyhow::Error;

    fn from_str(s: &str) -> Result<Self> {
        let name = RealmName(s.to_string());
        name.validate()?;
        Ok(name)
    }
}

impl Validate for RealmName {
    fn validate(&self) -> Result<(), ValidationErrors> {
        if Regex::new("^[a-z0-9]{4,32}$").unwrap().is_match(&self.0) {
            Ok(())
        } else {
            Err(ValidationErrors::new())
        }
    }
}

impl ToKey for RealmName {
    fn to_key(&self) -> native_db::Key {
        native_db::Key::new(self.0.as_bytes().to_vec())
    }

    fn key_names() -> Vec<String> {
        vec!["RealmName".to_string()]
    }
}

#[cfg(test)]
mod test_realm_name {
    use super::*;

    #[test]
    fn test_valid() {
        assert!("test".parse::<RealmName>().is_ok());
        assert!("1default".parse::<RealmName>().is_ok());
        assert!("default".parse::<RealmName>().is_ok());
        assert!("default99".parse::<RealmName>().is_ok());
    }

    #[test]
    fn test_invalid() {
        assert!("t".parse::<RealmName>().is_err());
        assert!("".parse::<RealmName>().is_err());
        assert!("test*".parse::<RealmName>().is_err());
        assert!(
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                .parse::<RealmName>()
                .is_err()
        );
    }
}

/// A user's username is forever unchangable.
#[derive(Serialize, Deserialize, Debug, Clone, PartialEq, Eq)]
pub struct UserName(String);

impl Deref for UserName {
    type Target = String;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl FromStr for UserName {
    type Err = anyhow::Error;

    fn from_str(s: &str) -> Result<Self> {
        let name = UserName(s.to_string());
        name.validate()?;
        Ok(name)
    }
}

impl Validate for UserName {
    fn validate(&self) -> Result<(), ValidationErrors> {
        if Regex::new("^[a-z0-9]{4,32}$").unwrap().is_match(&self.0) {
            Ok(())
        } else {
            Err(ValidationErrors::new())
        }
    }
}

impl Default for UserName {
    fn default() -> Self {
        UserName("admin".to_string())
    }
}

impl Display for UserName {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(&self.0)
    }
}

impl ToKey for UserName {
    fn to_key(&self) -> native_db::Key {
        native_db::Key::new(self.0.as_bytes().to_vec())
    }

    fn key_names() -> Vec<String> {
        vec!["UserName".to_string()]
    }
}
