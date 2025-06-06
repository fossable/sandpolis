use anyhow::Result;
use sandpolis_core::GroupName;

#[cfg(feature = "agent")]
pub mod agent;

pub mod messages;

#[derive(Clone)]
pub struct FilesystemLayer {}

impl FilesystemLayer {
    pub fn new() -> Result<Self> {
        Ok(Self {})
    }
}

pub enum FilesystemPermission {
    Read(Vec<GroupName>),
    Write(Vec<GroupName>),
    Mount(Vec<GroupName>),
}
