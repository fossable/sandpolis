use std::path::PathBuf;

#[cfg(feature = "agent")]
pub mod agent;

pub mod messages;

#[derive(Clone)]
pub struct FilesystemLayer {}
