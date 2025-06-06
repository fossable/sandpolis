use anyhow::Result;
use clap::Parser;
use native_db::ToKey;
use native_db::*;
use native_model::{Model, native_model};
use sandpolis_core::{ClusterId, InstanceId};
use sandpolis_database::{Data, DataIdentifier, DatabaseLayer};
use sandpolis_macros::Data;
use serde::{Deserialize, Serialize, de::DeserializeOwned};
use std::cmp::Ordering;

pub mod cli;

#[derive(Serialize, Deserialize, PartialEq, Debug, Clone, Data)]
#[native_model(id = 15, version = 1)]
#[native_db]
pub struct InstanceLayerData {
    #[primary_key]
    pub _id: DataIdentifier,

    pub cluster_id: ClusterId,
    pub instance_id: InstanceId,
    pub os_info: os_info::Info,
}

impl Default for InstanceLayerData {
    fn default() -> Self {
        Self {
            _id: DataIdentifier::default(),
            cluster_id: ClusterId::default(),
            instance_id: InstanceId::default(),
            os_info: os_info::get(),
        }
    }
}

#[derive(Clone)]
pub struct InstanceLayer {
    data: Watch<InstanceLayerData>,
    pub instance_id: InstanceId,
    pub cluster_id: ClusterId,
}

impl InstanceLayer {
    pub fn new(database: DatabaseLayer) -> Result<Self> {
        let db = database.get(None)?;
        let r = db.r_transaction()?;
        let instance_id = if let Some(data) = r.get().primary::<InstanceLayerData>(1)? {
            data.instance_id
        } else {
            let rw = db.rw_transaction()?;
            rw.insert(InstanceLayerData::default())?;
            rw.commit()?;
            todo!()
        };

        Ok(Self {
            database,
            instance_id,
            cluster_id: todo!(),
        })
    }
}

#[derive(Serialize, Deserialize, Clone, Debug, PartialEq, Eq, Ord)]
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

/// A config fragment that can take overrides from the command line or from
/// the process environment.
pub trait OverridableConfig<C>
where
    C: Parser,
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
