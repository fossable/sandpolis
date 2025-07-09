use anyhow::Result;
use native_db::ToKey;
use native_model::Model;
use sandpolis_core::{ClusterId, InstanceId, RealmName};
use sandpolis_database::{DatabaseLayer, Resident};
use sandpolis_macros::data;
use serde::{Deserialize, Serialize, de::DeserializeOwned};
use std::cmp::Ordering;

pub mod cli;

#[data]
pub struct InstanceLayerData {
    pub cluster_id: ClusterId,
    pub instance_id: InstanceId,
    pub os_info: os_info::Info,
}

#[derive(Clone)]
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
