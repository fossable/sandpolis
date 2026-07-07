use anyhow::Result;
use native_db::ToKey;
use native_model::Model;
use sandpolis_instance::InstanceLayer;
use sandpolis_instance::database::{DatabaseLayer, Resident};
use sandpolis_instance::realm::RealmName;
use sandpolis_macros::data;
#[cfg(feature = "agent")]
use std::sync::Arc;

#[cfg(feature = "client")]
pub mod client;

pub mod applications;
pub mod hardware;
pub mod os;
pub mod package;

#[data]
#[derive(Default)]
pub struct InventoryLayerData {}

#[cfg(feature = "agent")]
use tokio::sync::Mutex;

#[derive(Clone)]
pub struct InventoryLayer {
    #[allow(dead_code)]
    data: Resident<InventoryLayerData>,
    #[cfg(feature = "agent")]
    pub memory: Arc<Mutex<os::memory::agent::MemoryMonitor>>,
    #[cfg(feature = "agent")]
    pub users: Arc<Mutex<os::user::agent::UserCollector>>,
    #[cfg(feature = "agent")]
    pub packages: Arc<Mutex<package::agent::PackageCollector>>,
}

impl InventoryLayer {
    #[cfg_attr(not(feature = "agent"), allow(unused_variables))]
    pub async fn new(database: DatabaseLayer, instance: InstanceLayer) -> Result<Self> {
        Ok(Self {
            #[cfg(feature = "agent")]
            memory: Arc::new(Mutex::new(os::memory::agent::MemoryMonitor::new(
                database.realm(RealmName::default())?,
                instance.instance_id,
            )?)),
            #[cfg(feature = "agent")]
            users: Arc::new(Mutex::new(os::user::agent::UserCollector::new(
                database.realm(RealmName::default())?,
                instance.instance_id,
            )?)),
            #[cfg(feature = "agent")]
            packages: Arc::new(Mutex::new(package::agent::PackageCollector::new(
                database.realm(RealmName::default())?,
                instance.instance_id,
            )?)),
            data: database.realm(RealmName::default())?.resident(())?,
        })
    }
}
