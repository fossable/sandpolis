use std::sync::Arc;

use anyhow::Result;
use os::memory::agent::MemoryMonitor;
use sandpolis_database::Document;
use serde::{Deserialize, Serialize};

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
    pub memory: Arc<MemoryMonitor>,
}

impl SysinfoLayer {
    pub fn new(data: Document<SysinfoLayerData>) -> Result<Self> {
        Ok(Self {
            memory: Arc::new(MemoryMonitor::new(data.document("/memory")?)),
            data,
        })
    }
}
