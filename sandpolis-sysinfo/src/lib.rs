use anyhow::Result;
use native_db::ToKey;
use native_model::Model;
use sandpolis_database::{Data, DataIdentifier, DatabaseLayer, Resident};
use sandpolis_instance::InstanceLayer;
use sandpolis_macros::data;
use serde::{Deserialize, Serialize};
use std::sync::Arc;

#[cfg(feature = "agent")]
pub mod agent;

pub mod hardware;
pub mod os;

#[data]
pub struct SysinfoLayerData {}

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
            memory: Arc::new(os::memory::agent::MemoryMonitor::new(Resident::singleton(
                database.get(None).await?,
            )?)),
            #[cfg(feature = "agent")]
            users: Arc::new(os::user::agent::UserCollector::new(
                database.get(None).await?,
            )),
            data: Resident::singleton(database.get(None).await?)?,
        })
    }
}
