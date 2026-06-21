// doc_comment! {
//     include_str!("../README.md")
// }

use anyhow::Result;
use sandpolis_instance::database::DatabaseLayer;

pub mod bootagent;
#[cfg(not(target_os = "android"))]
pub mod cli;
pub mod config;
pub mod uefi;
pub mod wake;

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
