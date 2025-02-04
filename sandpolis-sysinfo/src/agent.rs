use super::os::memory::agent::MemoryMonitor;
use anyhow::Result;
use axum::Router;
use serde::{Deserialize, Serialize};

use sandpolis_database::Document;

pub fn router() -> Router {
    Router::new()
}
