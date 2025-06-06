use anyhow::Result;
use sandpolis_database::DatabaseLayer;
use serde::{Deserialize, Serialize};
use std::sync::Arc;

#[cfg(feature = "agent")]
pub mod agent;

pub mod hardware;
pub mod os;

#[derive(Serialize, Deserialize, Default, Clone)]
pub struct SysinfoLayerData {}

#[derive(Clone)]
pub struct SysinfoLayer {
    database: DatabaseLayer,
    #[cfg(feature = "agent")]
    pub memory: Arc<os::memory::agent::MemoryMonitor>,
    #[cfg(feature = "agent")]
    pub users: Arc<os::user::agent::UserCollector>,
}

impl SysinfoLayer {
    pub fn new(database: DatabaseLayer) -> Result<Self> {
        Ok(Self {
            #[cfg(feature = "agent")]
            memory: Arc::new(os::memory::agent::MemoryMonitor::new(
                data.document("/memory")?,
            )),
            #[cfg(feature = "agent")]
            users: Arc::new(os::user::agent::UserCollector::new()),
            database,
        })
    }
}
