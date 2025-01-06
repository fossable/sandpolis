use axum::Router;
use os::memory::MemoryMonitor;

pub mod os;

pub struct SysinfoLayer {
    pub memory: MemoryMonitor,
}

impl SysinfoLayer {
    pub fn new() -> Self {
        todo!()
    }
}

pub fn router() -> Router {
    Router::new()
}
