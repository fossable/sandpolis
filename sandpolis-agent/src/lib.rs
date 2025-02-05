use anyhow::Result;
use serde::Deserialize;
use serde::Serialize;

pub mod cli;

/// Polls data periodically.
pub trait Collector {
    fn refresh(&mut self) -> Result<()>;
    //start
    //stop
}
