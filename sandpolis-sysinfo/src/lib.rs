use anyhow::Result;
use native_db::*;
use native_model::{Model, native_model};
use sandpolis_database::{Data, DataIdentifier, DatabaseLayer, Resident};
use sandpolis_instance::InstanceLayer;
use sandpolis_macros::Data;
use serde::{Deserialize, Serialize};
use std::sync::Arc;

#[cfg(feature = "agent")]
pub mod agent;

pub mod hardware;
pub mod os;

#[derive(Serialize, Deserialize, Clone, Default, PartialEq, Debug, Data)]
#[native_model(id = 26, version = 1)]
#[native_db]
pub struct SysinfoLayerData {
    #[primary_key]
    pub _id: DataIdentifier,
}

#[derive(Clone)]
pub struct SysinfoLayer {
    data: Resident<SysinfoLayerData>,
    #[cfg(feature = "agent")]
    pub memory: Arc<os::memory::agent::MemoryMonitor>,
    #[cfg(feature = "agent")]
    pub users: Arc<os::user::agent::UserCollector>,
}

impl SysinfoLayer {
    pub async fn new(database: DatabaseLayer, instance: InstanceLayer) -> Result<Self> {
        Ok(Self {
            #[cfg(feature = "agent")]
            memory: Arc::new(os::memory::agent::MemoryMonitor::new(
                data.document("/memory")?,
            )),
            #[cfg(feature = "agent")]
            users: Arc::new(os::user::agent::UserCollector::new(
                database.get(None).await?,
            )),
            data: Resident::singleton(database.get(None).await?)?,
        })
    }
}
