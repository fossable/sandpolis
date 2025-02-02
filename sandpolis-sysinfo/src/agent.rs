use anyhow::Result;
use axum::Router;
use os::memory::MemoryMonitor;
use serde::{Deserialize, Serialize};

use sandpolis_database::Document;

#[derive(Serialize, Deserialize, Default)]
pub struct SysinfoData {}

pub struct SysinfoState {
    pub data: Document<SysinfoData>,
    pub memory: MemoryMonitor,
}

impl SysinfoState {
    pub fn new(data: Document<SysinfoData>) -> Result<Self> {
        Ok(Self {
            memory: MemoryMonitor::new(data.document("/memory")?),
            data,
        })
    }
}

pub fn router() -> Router {
    Router::new()
}
