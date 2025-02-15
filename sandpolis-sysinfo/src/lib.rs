use anyhow::Result;
use sandpolis_database::Document;
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
    pub data: Document<SysinfoLayerData>,
    #[cfg(feature = "agent")]
    pub memory: Arc<os::memory::agent::MemoryMonitor>,
}

impl SysinfoLayer {
    pub fn new(data: Document<SysinfoLayerData>) -> Result<Self> {
        Ok(Self {
            #[cfg(feature = "agent")]
            memory: Arc::new(os::memory::agent::MemoryMonitor::new(
                data.document("/memory")?,
            )),
            data,
        })
    }
}
