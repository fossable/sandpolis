use anyhow::Result;

pub mod screenshot;
pub mod session;

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
