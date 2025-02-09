use anyhow::Result;

#[cfg(feature = "agent")]
pub mod agent;
#[cfg(feature = "client")]
pub mod client;

pub mod messages;

#[derive(Clone)]
pub struct PowerLayer {}

impl PowerLayer {
    pub fn new() -> Result<Self> {
        Ok(Self {})
    }
}
