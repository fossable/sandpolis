use anyhow::Result;
use sandpolis_database::Document;
use serde::{Deserialize, Serialize};
use tokio_cron_scheduler::JobScheduler;

pub mod config;

#[cfg(feature = "agent")]
pub mod agent;

pub mod messages;

#[derive(Serialize, Deserialize, Default, Clone)]
pub struct AgentLayerData;

#[derive(Clone)]
pub struct AgentLayer {
    pub data: Document<AgentLayerData>,
    #[cfg(feature = "agent")]
    pub scheduler: JobScheduler,
}

impl AgentLayer {
    pub async fn new(data: Document<AgentLayerData>) -> Result<Self> {
        Ok(Self {
            data,
            #[cfg(feature = "agent")]
            scheduler: JobScheduler::new().await?,
        })
    }
}

/// Polls data periodically.
pub trait Collector {
    fn refresh(&mut self) -> Result<()>;
    //start
    //stop
}
