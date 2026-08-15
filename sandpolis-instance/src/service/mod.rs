//! Long-running background work that any subsystem can build on.
//!
//! A service implements [`Service`]: a name, the layer it belongs to, a
//! [`ServiceSchedule`], and one `async` [`run`](Service::run). The
//! [`ServiceRunner`] owns the schedule, supervises each service on its own
//! cancellable tokio task, and records what it does in [`ServiceData`] so the
//! state of background work is visible from a client without server log access.
//!
//! Both servers and agents host services. The runner is instance-agnostic; the
//! hosting instance is stamped on every row, so a client can tell an agent's
//! collectors apart from a server's scrapers.
//!
//! # Enabling and disabling
//!
//! [`ServiceData::enabled`] is the desired state and it lives in the database,
//! so a service switched off from the client stays off across a restart.
//! Subsystem configuration is a separate, coarser control: a service its
//! subsystem declines to register (because a config flag turned that whole
//! subsystem off) never reaches the runner at all and doesn't appear in the
//! client. That means turning a
//! config flag back on does *not* re-enable a service that was disabled from the
//! GUI — the stored `enabled` still applies.

use crate::LayerName;
use anyhow::Result;
use native_db::*;
use native_model::Model;
use sandpolis_macros::data;
use serde::{Deserialize, Serialize};
use std::future::Future;
use std::pin::Pin;
use std::time::Duration;
use tokio_util::sync::CancellationToken;

pub mod control;

#[cfg(any(feature = "server", feature = "agent"))]
mod runner;

#[cfg(any(feature = "server", feature = "agent"))]
pub use runner::{ServiceHandle, ServiceRunner, handle};

/// How a service's [`run`](Service::run) is driven.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ServiceSchedule {
    /// `run` is one bounded pass, invoked every `interval`.
    Periodic {
        interval: Duration,
        /// Whether the first pass happens immediately rather than after a full
        /// interval.
        run_at_startup: bool,
    },

    /// `run` is expected to occupy the service's whole lifetime. If it returns
    /// anyway, the supervisor starts it again after `restart_backoff`.
    Continuous { restart_backoff: Duration },
}

impl ServiceSchedule {
    /// A periodic schedule whose first pass runs at startup.
    pub fn every(interval: Duration) -> Self {
        Self::Periodic {
            interval,
            run_at_startup: true,
        }
    }

    /// How this schedule reads in the client.
    pub fn describe(&self) -> String {
        match self {
            Self::Periodic { interval, .. } => format!("every {}", humanize(*interval)),
            Self::Continuous { .. } => "continuous".to_string(),
        }
    }
}

/// Render a duration the way a person would say it, e.g. "30s" or "1h".
fn humanize(duration: Duration) -> String {
    let secs = duration.as_secs();
    match secs {
        0 => "0s".to_string(),
        s if s % 86400 == 0 => format!("{}d", s / 86400),
        s if s % 3600 == 0 => format!("{}h", s / 3600),
        s if s % 60 == 0 => format!("{}m", s / 60),
        s => format!("{s}s"),
    }
}

/// What one pass of a service accomplished.
#[derive(Debug, Default, Clone, Copy, PartialEq, Eq)]
pub struct ServiceReport {
    /// Items the pass considered.
    pub scanned: u64,
    /// Items it wrote new data for.
    pub updated: u64,
    /// Items it couldn't process. A pass with failed items is still a successful
    /// pass — individual targets go down all the time, and that shouldn't read
    /// as the service being broken.
    pub failed: u64,
}

/// A supervised unit of background work.
///
/// Implementations should be resumable and idempotent: a pass can be interrupted
/// at any point by a restart or by the service being disabled, and the next pass
/// has to cope. In practice that means deciding what to do from what's already
/// stored rather than from in-memory progress.
pub trait Service: Send + Sync + 'static {
    /// Stable name, unique within the service's layer. It forms half of the
    /// service's key, so changing it orphans the old [`ServiceData`] row.
    fn name(&self) -> &'static str;

    /// The layer this service belongs to. Decides which layer's toolbar surfaces
    /// it in the client.
    fn layer(&self) -> LayerName;

    /// One line explaining what the service does, shown in the client.
    fn description(&self) -> &'static str;

    fn schedule(&self) -> ServiceSchedule;

    /// Do the work.
    ///
    /// `cancel` fires when the service is disabled or the runner shuts down. A
    /// [`Continuous`](ServiceSchedule::Continuous) service must select on it; a
    /// periodic one may ignore it, since the supervisor drops the pass anyway.
    ///
    /// Returning `Err` marks the whole pass failed; a pass that merely couldn't
    /// reach some of its targets should report those in [`ServiceReport::failed`]
    /// and return `Ok`.
    fn run(&self, cancel: CancellationToken) -> impl Future<Output = Result<ServiceReport>> + Send;
}

/// The key identifying a service across the estate.
pub fn service_key(layer: &LayerName, name: &str) -> String {
    format!("{}/{}", layer.0, name)
}

/// What a service is currently doing.
#[derive(Serialize, Deserialize, Clone, Copy, Debug, PartialEq, Eq, Default)]
pub enum ServiceState {
    /// Not scheduled, because the service is disabled.
    #[default]
    Stopped,
    /// Scheduled, waiting for its next pass.
    Idle,
    /// A pass is in flight.
    Running,
    /// The last pass failed. The service is still scheduled.
    Failed,
}

impl std::fmt::Display for ServiceState {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        let s = match self {
            Self::Stopped => "stopped",
            Self::Idle => "idle",
            Self::Running => "running",
            Self::Failed => "failed",
        };
        f.write_str(s)
    }
}

/// A service's declared identity plus the outcome of its recent passes.
///
/// One row per service per hosting instance, updated after every pass. Synced to
/// clients so background work is inspectable from the GUI.
#[data(instance)]
#[derive(Default)]
pub struct ServiceData {
    /// `"{layer}/{name}"`. A single derived string because native_db has no
    /// compound secondary keys and identity here is really the pair.
    ///
    /// Only unique *per instance*: a server holds its own rows alongside every
    /// connected agent's, and two agents running the same collector share a key.
    /// Lookups always pair this with `_instance_id`.
    #[secondary_key]
    pub key: String,

    /// The layer that registered the service, for filtering by the client.
    #[secondary_key]
    pub layer: String,

    pub name: String,

    pub description: String,

    /// The schedule, rendered by [`ServiceSchedule::describe`].
    pub schedule: String,

    /// Desired state. Persisted, so a toggle from the client survives a restart.
    pub enabled: bool,

    pub state: ServiceState,

    /// Passes attempted.
    pub runs: u64,

    /// Passes that failed outright. Individual items failing doesn't count here
    /// — see `last_failed_items`.
    pub failures: u64,

    /// Items written across all passes.
    pub items_updated: u64,

    /// Items the last pass failed to process.
    pub last_failed_items: u64,

    /// When the last pass started, as milliseconds since the Unix epoch.
    pub last_run: Option<i64>,

    /// When the last successful pass finished.
    pub last_success: Option<i64>,

    /// Why the last failing pass failed. Cleared by the next success.
    pub last_error: Option<String>,
}

inventory::submit! {
    crate::database::sync::SyncRegistration(
        |r| r.register_scoped::<ServiceData>(|d| d._instance_id))
}

/// Object-safe view of [`Service`] so the runner can hold a heterogeneous list.
/// Implemented blanket-style, so services only ever write the `async fn`.
pub(crate) trait ErasedService: Send + Sync + 'static {
    fn name(&self) -> &'static str;
    fn layer(&self) -> LayerName;
    fn description(&self) -> &'static str;
    fn schedule(&self) -> ServiceSchedule;
    fn run<'a>(
        &'a self,
        cancel: CancellationToken,
    ) -> Pin<Box<dyn Future<Output = Result<ServiceReport>> + Send + 'a>>;
}

impl<T: Service> ErasedService for T {
    fn name(&self) -> &'static str {
        Service::name(self)
    }

    fn layer(&self) -> LayerName {
        Service::layer(self)
    }

    fn description(&self) -> &'static str {
        Service::description(self)
    }

    fn schedule(&self) -> ServiceSchedule {
        Service::schedule(self)
    }

    fn run<'a>(
        &'a self,
        cancel: CancellationToken,
    ) -> Pin<Box<dyn Future<Output = Result<ServiceReport>> + Send + 'a>> {
        Box::pin(Service::run(self, cancel))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn schedules_read_naturally() {
        assert_eq!(
            ServiceSchedule::every(Duration::from_secs(30)).describe(),
            "every 30s"
        );
        assert_eq!(
            ServiceSchedule::every(Duration::from_secs(300)).describe(),
            "every 5m"
        );
        assert_eq!(
            ServiceSchedule::every(Duration::from_secs(3600)).describe(),
            "every 1h"
        );
        assert_eq!(
            ServiceSchedule::every(Duration::from_secs(7 * 86400)).describe(),
            "every 7d"
        );
        assert_eq!(
            ServiceSchedule::Continuous {
                restart_backoff: Duration::from_secs(5)
            }
            .describe(),
            "continuous"
        );
    }
}
