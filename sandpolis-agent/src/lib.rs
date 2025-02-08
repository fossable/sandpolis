use anyhow::Result;
use serde::Deserialize;
use serde::Serialize;

pub mod cli;

#[cfg(feature = "agent")]
pub mod agent;

pub mod messages;

/// Polls data periodically.
pub trait Collector {
    fn refresh(&mut self) -> Result<()>;
    //start
    //stop
}
