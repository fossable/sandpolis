use anyhow::Result;
use sandpolis_database::Document;
use serde::{Deserialize, Serialize};
use tokio_cron_scheduler::JobScheduler;

pub mod routes;

#[derive(Serialize, Deserialize, Default, Clone)]
pub struct AgentLayerData;

#[derive(Clone)]
pub struct AgentLayer {
    pub data: Document<AgentLayerData>,
    pub scheduler: JobScheduler,
}

impl AgentLayer {
    pub async fn new(data: Document<AgentLayerData>) -> Result<Self> {
        Ok(Self {
            data,
            scheduler: JobScheduler::new().await?,
        })
    }
}
