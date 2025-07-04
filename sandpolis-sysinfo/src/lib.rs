use anyhow::Result;
use native_db::ToKey;
use native_model::Model;
use sandpolis_core::RealmName;
use sandpolis_database::{DatabaseLayer, Resident};
use sandpolis_instance::InstanceLayer;
use sandpolis_macros::data;
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
            memory: Arc::new(os::memory::agent::MemoryMonitor::new(
                database.realm(RealmName::default()).await?,
            )?),
            #[cfg(feature = "agent")]
            users: Arc::new(os::user::agent::UserCollector::new(
                database.realm(RealmName::default()).await?,
            )?),
            data: database.realm(RealmName::default()).await?.resident(())?,
        })
    }
}
