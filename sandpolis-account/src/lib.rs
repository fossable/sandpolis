//! This layer enables account-level management.

use anyhow::Result;
use native_db::*;
use native_model::{Model, native_model};
use sandpolis_core::InstanceId;
use sandpolis_database::{Data, DataIdentifier, DatabaseLayer, Watch};
use sandpolis_macros::Data;
use serde::{Deserialize, Serialize};
use validator::Validate;

#[derive(Serialize, Deserialize, Clone, Default, PartialEq, Debug, Data)]
#[native_model(id = 27, version = 1)]
#[native_db]
pub struct AccountLayerData {
    #[primary_key]
    pub _id: DataIdentifier,
}

#[derive(Clone)]
pub struct AccountLayer {
    database: DatabaseLayer,
}

impl AccountLayer {
    pub async fn new() -> Result<Self> {
        Ok(Self { database: todo!() })
    }
}

#[derive(Serialize, Deserialize, Clone, Copy, Debug, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct AccountId(u128);

#[derive(Serialize, Deserialize, Clone, Copy, Debug, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct AccountLinkId(u128);

/// A subjective indicator of how serious a compromise of an `Account` would be.
/// Accounts with higher ratings are more valuable than those with lower.
#[derive(Serialize, Deserialize, Clone, Copy, Debug, PartialEq, Eq, PartialOrd, Ord, Hash)]
pub struct AccountValue(i32);

impl Default for AccountValue {
    fn default() -> Self {
        Self(0)
    }
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct AccountData {
    service: Option<String>,
    username: Option<String>,
    email: Option<String>,
    value: AccountValue,
    /// An instance associated with this account
    instance: Option<InstanceId>,
    incoming: Vec<AccountLinkId>,
    outgoing: Vec<AccountLinkId>,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct AccountLinkData {
    r#type: AccountLinkType,
    incoming: AccountId,
    outgoing: AccountId,
}

/// Represents a directional or bidirectional relationship between two accounts.
#[derive(Serialize, Deserialize, Clone, Debug)]
pub enum AccountLinkType {
    CommonEmail(String),
    CommonPassword {
        /// Whether multiple authentication factors are required
        mfa: Option<bool>,
    },
    /// The accounts share a username for authentication
    CommonUsername(String),
    Recovery,
    SshAuthorizedKey {
        /// Whether the SSH key is stored encrypted
        encrypted: Option<bool>,
    },
}

/// A subjective indicator of how likely a compromise of an `Account` can
/// occur given some criteria (as a decimal percentage).
#[derive(Serialize, Deserialize, Clone, Copy, Debug, PartialEq, PartialOrd)]
pub struct Compromisability(f32);

impl Validate for Compromisability {
    fn validate(&self) -> Result<(), validator::ValidationErrors> {
        // Between 0 and 1
        todo!()
    }
}

impl AccountLinkType {
    pub fn compromisability(&self) -> Compromisability {
        // Weights are relative
        match self {
            AccountLinkType::CommonUsername(_) => Compromisability(0.05),
            AccountLinkType::CommonEmail(_) => Compromisability(0.10),
            AccountLinkType::CommonPassword { mfa } => match mfa {
                Some(true) => Compromisability(0.20),
                Some(false) => Compromisability(0.80),
                None => Compromisability(0.50),
            },
            AccountLinkType::Recovery => Compromisability(0.95),
            AccountLinkType::SshAuthorizedKey { encrypted } => match encrypted {
                Some(true) => Compromisability(0.20),
                Some(false) => Compromisability(0.95),
                None => Compromisability(0.50),
            },
        }
    }
}
