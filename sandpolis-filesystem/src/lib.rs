use anyhow::Result;

#[cfg(feature = "client")]
pub mod client;

pub mod session;

#[derive(Clone)]
pub struct FilesystemManager {}

impl FilesystemManager {
    pub async fn new() -> Result<Self> {
        Ok(Self {})
    }
}

// What a client must be granted to open this layer's streams.
inventory::submit! {
    sandpolis_instance::network::stream::StreamPermission::require(
        sandpolis_macros::stream_tag!(FsSessionStream), "filesystem:session")
}
