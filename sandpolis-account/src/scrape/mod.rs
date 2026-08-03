//! Foundations for periodic server-side tasks that scrape data from the
//! Internet.
//!
//! A task implements [`ScrapeTask`]: a name, how often it runs, and one `async`
//! pass that reads whatever it needs out of the realm database, fetches from the
//! network through the shared [`HttpFetcher`], and writes results back. The
//! [`ScrapeRunner`] owns the schedule, enforces the shared request budget, and
//! records every pass in [`ScrapeTaskData`] so runs are inspectable from the
//! client's database browser.
//!
//! Only servers scrape. Agents have no reason to reach out to third parties on
//! the estate's behalf, and having every client do it independently would
//! multiply the traffic for no benefit.

use native_db::*;
use native_model::Model;
use sandpolis_macros::data;

#[cfg(feature = "server")]
mod runner;

#[cfg(feature = "server")]
pub use runner::{Fetched, HttpFetcher, ScrapeContext, ScrapeReport, ScrapeRunner, ScrapeTask};

/// The outcome of a scraping task's most recent passes.
///
/// One row per task, updated after every pass. Synced to clients so the state of
/// background scraping is visible without server log access.
#[data]
#[derive(Default)]
pub struct ScrapeTaskData {
    /// The task's stable name, e.g. "favicon".
    #[secondary_key(unique)]
    pub task: String,

    /// Passes attempted.
    pub runs: u64,

    /// Passes that failed outright. Individual items failing (a site being down)
    /// doesn't count here — see `last_failed_items`.
    pub failures: u64,

    /// Items written across all passes.
    pub items_updated: u64,

    /// Items the last pass failed to scrape.
    pub last_failed_items: u64,

    /// When the last pass started, as milliseconds since the Unix epoch.
    pub last_run: Option<i64>,

    /// When the last successful pass finished.
    pub last_success: Option<i64>,

    /// Why the last failing pass failed. Cleared by the next success.
    pub last_error: Option<String>,
}

inventory::submit! {
    sandpolis_instance::database::sync::SyncRegistration(|r| r.register::<ScrapeTaskData>())
}
