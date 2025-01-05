use axum::Router;
use os::memory::MemoryMonitor;

pub mod os;

pub struct SysinfoLayer {
    pub memory: MemoryMonitor,
}

pub fn router() -> Router {
    Router::new()
}
