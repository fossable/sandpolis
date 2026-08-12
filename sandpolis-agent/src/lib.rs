// doc_comment! {
//     include_str!("../README.md")
// }

use anyhow::Result;
use sandpolis_instance::database::DatabaseLayer;

pub mod bootagent;
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
///
/// A collector doesn't own its schedule. An agent layer wraps each one in a
/// [`CollectorService`] and hands it to the service runner, which decides when —
/// and whether — `refresh` is called.
pub trait Collector: Send + 'static {
    /// Written as `-> impl Future` rather than `async fn` so the `Send` bound is
    /// part of the contract; [`CollectorService`] is generic over collectors and
    /// can't otherwise prove its own futures are `Send`.
    fn refresh(&mut self) -> impl Future<Output = Result<()>> + Send;
}

/// Runs a [`Collector`] on an interval as a supervised service.
#[cfg(feature = "agent")]
pub struct CollectorService<C> {
    collector: std::sync::Arc<tokio::sync::Mutex<C>>,
    layer: sandpolis_instance::LayerName,
    name: &'static str,
    description: &'static str,
    interval: std::time::Duration,
}

#[cfg(feature = "agent")]
impl<C: Collector> CollectorService<C> {
    /// Wrap `collector`, which the layer keeps a handle to, in a service
    /// belonging to `layer`.
    pub fn new(
        collector: std::sync::Arc<tokio::sync::Mutex<C>>,
        layer: impl Into<sandpolis_instance::LayerName>,
        name: &'static str,
        description: &'static str,
        interval: std::time::Duration,
    ) -> Self {
        Self {
            collector,
            layer: layer.into(),
            name,
            description,
            interval,
        }
    }
}

#[cfg(feature = "agent")]
impl<C: Collector> sandpolis_instance::service::Service for CollectorService<C> {
    fn name(&self) -> &'static str {
        self.name
    }

    fn layer(&self) -> sandpolis_instance::LayerName {
        self.layer.clone()
    }

    fn description(&self) -> &'static str {
        self.description
    }

    fn schedule(&self) -> sandpolis_instance::service::ServiceSchedule {
        sandpolis_instance::service::ServiceSchedule::every(self.interval)
    }

    async fn run(
        &self,
        _: tokio_util::sync::CancellationToken,
    ) -> Result<sandpolis_instance::service::ServiceReport> {
        self.collector.lock().await.refresh().await?;
        // A collector writes whatever it found straight to the database without
        // saying how much, so there's nothing honest to report but the pass
        // itself. Failures still surface through the returned error.
        Ok(sandpolis_instance::service::ServiceReport::default())
    }
}
