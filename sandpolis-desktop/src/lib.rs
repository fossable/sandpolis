use anyhow::Result;

pub(crate) mod messages;

#[cfg(feature = "agent")]
pub mod agent;

#[cfg(feature = "client")]
pub mod client;

#[derive(Clone)]
pub struct DesktopLayer {}

impl DesktopLayer {
    pub async fn new() -> Result<Self> {
        Ok(Self {})
    }
}

// TODO agent lists available desktops for db
