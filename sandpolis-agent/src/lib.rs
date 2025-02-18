use anyhow::Result;
use sandpolis_database::Document;
use serde::{Deserialize, Serialize};

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
    pub scheduler: tokio_cron_scheduler::JobScheduler,
}

impl AgentLayer {
    pub async fn new(data: Document<AgentLayerData>) -> Result<Self> {
        Ok(Self {
            data,
            #[cfg(feature = "agent")]
            scheduler: tokio_cron_scheduler::JobScheduler::new().await?,
        })
    }
}

/// Polls data periodically.
pub trait Collector {
    fn refresh(&mut self) -> Result<()>;
    //start
    //stop
}
