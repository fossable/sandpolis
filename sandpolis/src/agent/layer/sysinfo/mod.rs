use anyhow::Result;
use axum::Router;
use os::memory::MemoryMonitor;
use serde::{Deserialize, Serialize};

use crate::core::database::Document;

pub mod os;

#[derive(Serialize, Deserialize, Default)]
pub struct SysinfoLayerData {}

pub struct SysinfoLayer {
    pub data: Document<SysinfoLayerData>,
    pub memory: MemoryMonitor,
}

impl SysinfoLayer {
    pub fn new(data: Document<SysinfoLayerData>) -> Result<Self> {
        Ok(Self {
            memory: MemoryMonitor::new(data.document("/memory")?),
            data,
        })
    }
}

pub fn router() -> Router {
    Router::new()
}
