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
pub mod package;

#[data]
#[derive(Default)]
pub struct InventoryLayerData {}

#[derive(Clone)]
pub struct InventoryLayer {
    data: Resident<InventoryLayerData>,
    #[cfg(feature = "agent")]
    pub memory: Arc<os::memory::agent::MemoryMonitor>,
    #[cfg(feature = "agent")]
    pub users: Arc<os::user::agent::UserCollector>,
}

impl InventoryLayer {
    pub async fn new(database: DatabaseLayer, instance: InstanceLayer) -> Result<Self> {
        Ok(Self {
            #[cfg(feature = "agent")]
            memory: Arc::new(os::memory::agent::MemoryMonitor::new(
                database.realm(RealmName::default())?,
            )?),
            #[cfg(feature = "agent")]
            users: Arc::new(os::user::agent::UserCollector::new(
                database.realm(RealmName::default())?,
            )?),
            data: database.realm(RealmName::default())?.resident(())?,
        })
    }

    pub fn pacman() {}
}
