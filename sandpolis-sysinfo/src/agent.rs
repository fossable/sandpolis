use super::os::memory::agent::MemoryMonitor;
use anyhow::Result;
use axum::Router;
use serde::{Deserialize, Serialize};

pub fn router() -> Router {
    Router::new()
}
