use anyhow::Result;
#[cfg(feature = "server")]
use headers::{Header, HeaderName, HeaderValue};
use native_db::ToKey;
use regex::Regex;
use serde::{Deserialize, Serialize, de::DeserializeOwned};
use std::fmt::{Display, Write};
use std::ops::Deref;
use std::str::FromStr;
use std::sync::LazyLock;
use strum::{EnumIter, IntoEnumIterator};
use uuid::Uuid;
use validator::{Validate, ValidationErrors};

static REALM_NAME_REGEX: LazyLock<Regex> =
    LazyLock::new(|| Regex::new("^[a-z0-9]{4,32}$").unwrap());
static USER_NAME_REGEX: LazyLock<Regex> = LazyLock::new(|| Regex::new("^[a-z0-9]{4,32}$").unwrap());

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
        if REALM_NAME_REGEX.is_match(&self.0) {
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

#[cfg(feature = "server")]
impl Header for RealmName {
    fn name() -> &'static HeaderName {
        static NAME: HeaderName = HeaderName::from_static("x-realm");
        &NAME
    }

    fn decode<'i, I>(values: &mut I) -> Result<Self, headers::Error>
    where
        I: Iterator<Item = &'i HeaderValue>,
    {
        Ok(values
            .next()
            .ok_or_else(headers::Error::invalid)?
            .to_str()
            .map_err(|_| headers::Error::invalid())?
            .parse()
            .map_err(|_| headers::Error::invalid())?)
    }

    fn encode<E>(&self, values: &mut E)
    where
        E: Extend<HeaderValue>,
    {
        values.extend(std::iter::once(
            HeaderValue::from_str(&self.to_string()).expect("Realm names only allow ascii 32-127"),
        ));
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
        if USER_NAME_REGEX.is_match(&self.0) {
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

#[cfg(test)]
mod test_user_name {
    use super::*;

    #[test]
    fn test_valid_usernames() {
        assert!("test".parse::<UserName>().is_ok());
        assert!("admin".parse::<UserName>().is_ok());
        assert!("user123".parse::<UserName>().is_ok());
        assert!("1234".parse::<UserName>().is_ok());
        assert!("abcd".parse::<UserName>().is_ok());
        assert!("user0".parse::<UserName>().is_ok());
        assert!("0user".parse::<UserName>().is_ok());
        assert!(
            "longusername1234567890123456789012"
                .parse::<UserName>()
                .is_ok()
        );
    }

    #[test]
    fn test_invalid_usernames() {
        // Too short
        assert!("a".parse::<UserName>().is_err());
        assert!("ab".parse::<UserName>().is_err());
        assert!("abc".parse::<UserName>().is_err());
        assert!("".parse::<UserName>().is_err());

        // Too long
        assert!(
            "verylongusernamethatexceedsthemaximumlengthallowed"
                .parse::<UserName>()
                .is_err()
        );
        assert!("a".repeat(33).parse::<UserName>().is_err());

        // Invalid characters
        assert!("user-name".parse::<UserName>().is_err());
        assert!("user_name".parse::<UserName>().is_err());
        assert!("user.name".parse::<UserName>().is_err());
        assert!("user@name".parse::<UserName>().is_err());
        assert!("user name".parse::<UserName>().is_err());
        assert!("User".parse::<UserName>().is_err());
        assert!("USER".parse::<UserName>().is_err());
        assert!("user!".parse::<UserName>().is_err());
        assert!("user#".parse::<UserName>().is_err());
        assert!("user$".parse::<UserName>().is_err());
        assert!("user%".parse::<UserName>().is_err());
        assert!("user^".parse::<UserName>().is_err());
        assert!("user&".parse::<UserName>().is_err());
        assert!("user*".parse::<UserName>().is_err());
        assert!("user(".parse::<UserName>().is_err());
        assert!("user)".parse::<UserName>().is_err());
        assert!("user+".parse::<UserName>().is_err());
        assert!("user=".parse::<UserName>().is_err());
        assert!("user[".parse::<UserName>().is_err());
        assert!("user]".parse::<UserName>().is_err());
        assert!("user{".parse::<UserName>().is_err());
        assert!("user}".parse::<UserName>().is_err());
        assert!("user|".parse::<UserName>().is_err());
        assert!("user\\".parse::<UserName>().is_err());
        assert!("user:".parse::<UserName>().is_err());
        assert!("user;".parse::<UserName>().is_err());
        assert!("user\"".parse::<UserName>().is_err());
        assert!("user'".parse::<UserName>().is_err());
        assert!("user<".parse::<UserName>().is_err());
        assert!("user>".parse::<UserName>().is_err());
        assert!("user,".parse::<UserName>().is_err());
        assert!("user?".parse::<UserName>().is_err());
        assert!("user/".parse::<UserName>().is_err());
        assert!("user~".parse::<UserName>().is_err());
        assert!("user`".parse::<UserName>().is_err());
    }

    #[test]
    fn test_boundary_lengths() {
        // Exactly 4 characters (minimum)
        assert!("test".parse::<UserName>().is_ok());
        assert!("1234".parse::<UserName>().is_ok());
        assert!("abcd".parse::<UserName>().is_ok());

        // Exactly 32 characters (maximum)
        let max_length = "a".repeat(32);
        assert_eq!(max_length.len(), 32);
        assert!(max_length.parse::<UserName>().is_ok());

        // Just over 32 characters
        let over_max = "a".repeat(33);
        assert_eq!(over_max.len(), 33);
        assert!(over_max.parse::<UserName>().is_err());

        // Just under 4 characters
        assert!("abc".parse::<UserName>().is_err());
    }

    #[test]
    fn test_default() {
        let default_username = UserName::default();
        assert_eq!(default_username.to_string(), "admin");
        assert_eq!(*default_username, "admin");
    }

    #[test]
    fn test_display() {
        let username = UserName("testuser".to_string());
        assert_eq!(username.to_string(), "testuser");
        assert_eq!(format!("{}", username), "testuser");
    }

    #[test]
    fn test_deref() {
        let username = UserName("testuser".to_string());
        assert_eq!(username.len(), 8);
        assert_eq!(username.chars().count(), 8);
        assert!(username.starts_with("test"));
        assert!(username.ends_with("user"));
    }

    #[test]
    fn test_equality() {
        let username1 = UserName("testuser".to_string());
        let username2 = UserName("testuser".to_string());
        let username3 = UserName("different".to_string());

        assert_eq!(username1, username2);
        assert_ne!(username1, username3);
    }
}

/// A layer represents a functional area of Sandpolis (e.g., filesystem, shell, network).
/// Layers are registered at runtime using the `inventory` crate.
#[cfg_attr(feature = "client-gui", derive(bevy::prelude::Resource))]
#[derive(Debug, Clone, PartialEq, Eq, Hash, PartialOrd, Ord, Serialize, Deserialize)]
pub struct Layer(pub String);

impl Layer {
    /// Create a new layer with the given name.
    pub const fn new(name: String) -> Self {
        Self(name)
    }

    /// Get the layer name.
    pub fn name(&self) -> &str {
        &self.0
    }

    /// Iterate over all registered layers.
    pub fn registered() -> impl Iterator<Item = &'static Layer> {
        inventory::iter::<Layer>()
    }
}

impl std::fmt::Display for Layer {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(&self.0)
    }
}

impl std::ops::Deref for Layer {
    type Target = str;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl From<&str> for Layer {
    fn from(s: &str) -> Self {
        Self(s.to_string())
    }
}

impl From<String> for Layer {
    fn from(s: String) -> Self {
        Self(s)
    }
}

impl PartialEq<str> for Layer {
    fn eq(&self, other: &str) -> bool {
        self.0 == other
    }
}

impl PartialEq<&str> for Layer {
    fn eq(&self, other: &&str) -> bool {
        self.0 == *other
    }
}

// Register Layer with inventory for runtime collection
inventory::collect!(Layer);

/// A config fragment that can take overrides from the command line or from
/// environment variables.
#[cfg(feature = "default")]
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
