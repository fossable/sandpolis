use anyhow::Result;
use native_db::ToKey;
use native_model::Model;
use sandpolis_core::{ClusterId, InstanceId, RealmName};
use sandpolis_database::{DatabaseLayer, Resident};
use sandpolis_macros::data;
use serde::{Deserialize, Serialize};
use std::cmp::Ordering;

pub mod cli;
pub mod config;

#[cfg(feature = "default")]
#[data]
#[derive(Default)]
pub struct InstanceLayerData {
    pub cluster_id: ClusterId,
    pub instance_id: InstanceId,
    pub os_info: os_info::Info,
}

#[derive(Clone)]
pub struct InstanceLayer {
    #[cfg(feature = "default")]
    data: Resident<InstanceLayerData>,
    pub instance_id: InstanceId,
    pub cluster_id: ClusterId,
}

impl InstanceLayer {
    #[cfg(feature = "default")]
    pub async fn new(database: DatabaseLayer) -> Result<Self> {
        let data: Resident<InstanceLayerData> =
            database.realm(RealmName::default())?.resident(())?;

        Ok(Self {
            instance_id: { data.read().instance_id },
            cluster_id: { data.read().cluster_id },
            data,
        })
    }

    #[cfg(not(feature = "default"))]
    pub fn new(cluster_id: ClusterId, instance_id: InstanceId) -> Result<Self> {
        Ok(Self {
            instance_id,
            cluster_id,
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
