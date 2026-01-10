// doc_comment! {
//     include_str!("../README.md")
// }

use anyhow::Result;
use sandpolis_database::DatabaseLayer;

pub mod config;

#[cfg(feature = "agent")]
pub mod agent;

pub mod messages;

pub mod uefi;

#[derive(Default)]
pub struct AgentLayerData {}

#[derive(Clone)]
pub struct AgentLayer {
    database: DatabaseLayer,
    #[cfg(feature = "agent")]
    pub scheduler: tokio_cron_scheduler::JobScheduler,
}

impl AgentLayer {
    pub async fn new(database: DatabaseLayer) -> Result<Self> {
        Ok(Self {
            database,
            #[cfg(feature = "agent")]
            scheduler: tokio_cron_scheduler::JobScheduler::new().await?,
        })
    }
}

/// Polls data periodically.
pub trait Collector {
    async fn refresh(&mut self) -> Result<()>;
    //start
    //stop
}
