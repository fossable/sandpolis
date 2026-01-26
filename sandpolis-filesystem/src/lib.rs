use anyhow::Result;
use sandpolis_core::RealmName;

#[cfg(feature = "client")]
pub mod client;

pub mod session;

#[derive(Clone)]
pub struct FilesystemLayer {}

impl FilesystemLayer {
    pub async fn new() -> Result<Self> {
        Ok(Self {})
    }
}

pub enum FilesystemPermission {
    Read(Vec<RealmName>),
    Write(Vec<RealmName>),
    Mount(Vec<RealmName>),
}
