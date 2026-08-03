//! The scraping task trait, its shared context, and the runner that schedules
//! tasks and records their results.

use super::ScrapeTaskData;
use crate::config::ScrapeConfig;
use anyhow::{Result, bail};
use chrono::Utc;
use sandpolis_instance::database::RealmDatabase;
use std::future::Future;
use std::pin::Pin;
use std::sync::Arc;
use std::time::Duration;
use tokio::sync::Semaphore;
use tokio::time::MissedTickBehavior;
use tracing::{debug, info, warn};
use url::Url;

/// What one pass of a task accomplished.
#[derive(Debug, Default, Clone, Copy, PartialEq, Eq)]
pub struct ScrapeReport {
    /// Items the pass considered.
    pub scanned: u64,
    /// Items it wrote new data for.
    pub updated: u64,
    /// Items it couldn't scrape. A pass with failed items is still a successful
    /// pass — individual sites go down all the time, and that shouldn't read as
    /// the task being broken.
    pub failed: u64,
}

/// A periodic task that scrapes data from the Internet into the database.
///
/// Implementations should be resumable and idempotent: a pass can be interrupted
/// at any point by a server restart, and the next pass has to cope. In practice
/// that means deciding what to fetch from what's already stored (as the favicon
/// task does with its staleness check) rather than from in-memory progress.
pub trait ScrapeTask: Send + Sync + 'static {
    /// Stable name, used for logging and as the key of this task's
    /// [`ScrapeTaskData`] row. Changing it orphans the old row.
    fn name(&self) -> &'static str;

    /// How long to wait between passes.
    fn interval(&self) -> Duration;

    /// Whether the first pass runs immediately at startup rather than after a
    /// full interval. Default is to run immediately.
    fn run_at_startup(&self) -> bool {
        true
    }

    /// Perform one pass.
    ///
    /// Returning `Err` marks the whole pass failed; a pass that merely couldn't
    /// reach some of its targets should report those in
    /// [`ScrapeReport::failed`] and return `Ok`.
    fn run(&self, ctx: &ScrapeContext) -> impl Future<Output = Result<ScrapeReport>> + Send;
}

/// Everything a task is given to do its work.
pub struct ScrapeContext {
    /// The realm the task reads from and writes to.
    pub realm: RealmDatabase,
    /// Shared, rate-limited HTTP access.
    pub http: HttpFetcher,
}

/// A response body, already checked against the configured size limit.
#[derive(Debug, Clone)]
pub struct Fetched {
    /// The URL the body actually came from, after any redirects.
    pub url: Url,
    pub content_type: Option<String>,
    pub body: Vec<u8>,
}

impl Fetched {
    /// The body as UTF-8, lossily decoded.
    pub fn text(&self) -> std::borrow::Cow<'_, str> {
        String::from_utf8_lossy(&self.body)
    }
}

/// Shared HTTP access for scraping tasks: one connection pool, one request
/// budget, one response size limit.
#[derive(Clone)]
pub struct HttpFetcher {
    client: reqwest::Client,
    /// Caps outbound requests in flight across every task, so adding tasks
    /// doesn't multiply the load we put on the network (or on any one site).
    budget: Arc<Semaphore>,
    max_bytes: usize,
}

impl HttpFetcher {
    pub fn new(config: &ScrapeConfig) -> Result<Self> {
        let client = reqwest::Client::builder()
            .user_agent(config.user_agent.clone())
            .timeout(Duration::from_secs(config.request_timeout))
            // Scraping follows links into untrusted territory; a redirect chain
            // is fine but shouldn't be unbounded.
            .redirect(reqwest::redirect::Policy::limited(5))
            .build()?;

        Ok(Self {
            client,
            budget: Arc::new(Semaphore::new(config.max_concurrent_requests.max(1))),
            max_bytes: config.max_response_bytes,
        })
    }

    /// GET `url`, reading at most the configured number of bytes.
    ///
    /// The body is read incrementally and abandoned as soon as it exceeds the
    /// limit, so an endless response can't exhaust memory even when it lies
    /// about (or omits) its content length.
    pub async fn get(&self, url: &Url) -> Result<Fetched> {
        if !matches!(url.scheme(), "http" | "https") {
            bail!("Refusing to fetch non-HTTP URL: {url}");
        }

        let _permit = self.budget.acquire().await?;

        let response = self.client.get(url.clone()).send().await?;
        let status = response.status();
        if !status.is_success() {
            bail!("{url} returned {status}");
        }

        let final_url = response.url().clone();
        let content_type = response
            .headers()
            .get(reqwest::header::CONTENT_TYPE)
            .and_then(|v| v.to_str().ok())
            .map(|v| v.to_string());

        let mut response = response;
        let mut body = Vec::new();
        while let Some(chunk) = response.chunk().await? {
            if body.len() + chunk.len() > self.max_bytes {
                bail!("{url} exceeds the {} byte response limit", self.max_bytes);
            }
            body.extend_from_slice(&chunk);
        }

        Ok(Fetched {
            url: final_url,
            content_type,
            body,
        })
    }
}

/// Schedules [`ScrapeTask`]s and records what they do.
pub struct ScrapeRunner {
    ctx: Arc<ScrapeContext>,
    tasks: Vec<Box<dyn ErasedScrapeTask>>,
}

impl ScrapeRunner {
    pub fn new(realm: RealmDatabase, config: &ScrapeConfig) -> Result<Self> {
        Ok(Self {
            ctx: Arc::new(ScrapeContext {
                realm,
                http: HttpFetcher::new(config)?,
            }),
            tasks: Vec::new(),
        })
    }

    /// Add a task to the schedule.
    pub fn register(&mut self, task: impl ScrapeTask) -> &mut Self {
        self.tasks.push(Box::new(task));
        self
    }

    /// The context tasks will be given. Exposed for tests, which drive a task's
    /// `run` directly rather than going through the schedule.
    pub fn context(&self) -> &Arc<ScrapeContext> {
        &self.ctx
    }

    /// Spawn every registered task on its own schedule.
    ///
    /// Each task gets an independent tokio task, so a slow or wedged one can't
    /// hold up the others. They run until the process exits.
    pub fn spawn(self) {
        if self.tasks.is_empty() {
            debug!("No scraping tasks registered");
            return;
        }

        for task in self.tasks {
            let ctx = self.ctx.clone();
            info!(
                task = task.name(),
                interval = ?task.interval(),
                "Scheduling scrape task"
            );
            tokio::spawn(async move { run_forever(ctx, task).await });
        }
    }
}

/// Run one task on its interval, forever.
async fn run_forever(ctx: Arc<ScrapeContext>, task: Box<dyn ErasedScrapeTask>) {
    if !task.run_at_startup() {
        tokio::time::sleep(task.interval()).await;
    }

    let mut ticker = tokio::time::interval(task.interval());
    // A pass that overruns its interval shouldn't cause a burst of catch-up
    // passes against the sites we just finished hammering.
    ticker.set_missed_tick_behavior(MissedTickBehavior::Delay);

    loop {
        ticker.tick().await;

        let started = Utc::now().timestamp_millis();
        let result = task.run(&ctx).await;

        match &result {
            Ok(report) => debug!(
                task = task.name(),
                scanned = report.scanned,
                updated = report.updated,
                failed = report.failed,
                "Scrape pass finished"
            ),
            Err(e) => warn!(task = task.name(), error = %e, "Scrape pass failed"),
        }

        // Bookkeeping is best-effort: losing a run record is not a reason to
        // stop scraping.
        if let Err(e) = record_pass(&ctx.realm, task.name(), started, &result) {
            debug!(task = task.name(), error = %e, "Failed to record scrape pass");
        }
    }
}

/// Fold one pass's outcome into the task's [`ScrapeTaskData`] row.
fn record_pass(
    realm: &RealmDatabase,
    name: &str,
    started: i64,
    result: &Result<ScrapeReport>,
) -> Result<()> {
    let rw = realm.rw_transaction()?;

    let existing: Vec<ScrapeTaskData> = rw
        .scan()
        .primary::<ScrapeTaskData>()?
        .all()?
        .collect::<std::result::Result<Vec<_>, _>>()?;
    let previous = existing.into_iter().find(|d| d.task == name);

    let mut updated = previous.clone().unwrap_or_else(|| ScrapeTaskData {
        task: name.to_string(),
        ..Default::default()
    });

    updated.runs += 1;
    updated.last_run = Some(started);
    match result {
        Ok(report) => {
            updated.items_updated += report.updated;
            updated.last_failed_items = report.failed;
            updated.last_success = Some(Utc::now().timestamp_millis());
            updated.last_error = None;
        }
        Err(e) => {
            updated.failures += 1;
            updated.last_error = Some(e.to_string());
        }
    }

    match previous {
        Some(previous) => {
            rw.upsert(ScrapeTaskData {
                // Keep the row's identity so this replaces rather than duplicates.
                _id: previous._id,
                ..updated
            })?;
        }
        None => {
            rw.insert(updated)?;
        }
    }

    rw.commit()?;
    Ok(())
}

/// Object-safe view of [`ScrapeTask`] so the runner can hold a heterogeneous
/// list. Implemented blanket-style, so tasks only ever write the `async fn`.
trait ErasedScrapeTask: Send + Sync + 'static {
    fn name(&self) -> &'static str;
    fn interval(&self) -> Duration;
    fn run_at_startup(&self) -> bool;
    fn run<'a>(
        &'a self,
        ctx: &'a ScrapeContext,
    ) -> Pin<Box<dyn Future<Output = Result<ScrapeReport>> + Send + 'a>>;
}

impl<T: ScrapeTask> ErasedScrapeTask for T {
    fn name(&self) -> &'static str {
        ScrapeTask::name(self)
    }

    fn interval(&self) -> Duration {
        ScrapeTask::interval(self)
    }

    fn run_at_startup(&self) -> bool {
        ScrapeTask::run_at_startup(self)
    }

    fn run<'a>(
        &'a self,
        ctx: &'a ScrapeContext,
    ) -> Pin<Box<dyn Future<Output = Result<ScrapeReport>> + Send + 'a>> {
        Box::pin(ScrapeTask::run(self, ctx))
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use sandpolis_instance::database::DatabaseLayer;
    use sandpolis_instance::realm::RealmName;
    use sandpolis_instance::test_db;

    fn realm() -> Result<RealmDatabase> {
        let db: DatabaseLayer = test_db!(ScrapeTaskData);
        db.realm(RealmName::default())
    }

    fn row(realm: &RealmDatabase, name: &str) -> Result<ScrapeTaskData> {
        let r = realm.r_transaction()?;
        let all: Vec<ScrapeTaskData> = r
            .scan()
            .primary::<ScrapeTaskData>()?
            .all()?
            .collect::<std::result::Result<Vec<_>, _>>()?;
        assert_eq!(all.len(), 1, "expected exactly one row: {all:#?}");
        Ok(all
            .into_iter()
            .find(|d| d.task == name)
            .expect("row exists"))
    }

    #[test]
    fn passes_accumulate_into_one_row() -> Result<()> {
        let realm = realm()?;

        record_pass(
            &realm,
            "favicon",
            1,
            &Ok(ScrapeReport {
                scanned: 3,
                updated: 2,
                failed: 1,
            }),
        )?;
        let first = row(&realm, "favicon")?;
        assert_eq!(first.runs, 1);
        assert_eq!(first.items_updated, 2);
        assert_eq!(first.last_failed_items, 1);
        assert_eq!(first.last_run, Some(1));
        assert!(first.last_success.is_some());
        assert_eq!(first.last_error, None);

        // A failing pass counts as a run and a failure, and records why.
        record_pass(&realm, "favicon", 2, &Err(anyhow::anyhow!("boom")))?;
        let second = row(&realm, "favicon")?;
        assert_eq!(second.runs, 2);
        assert_eq!(second.failures, 1);
        // Totals carry over rather than resetting.
        assert_eq!(second.items_updated, 2);
        assert_eq!(second.last_run, Some(2));
        assert_eq!(second.last_success, first.last_success);
        assert_eq!(second.last_error.as_deref(), Some("boom"));

        // The next success clears the error.
        record_pass(
            &realm,
            "favicon",
            3,
            &Ok(ScrapeReport {
                scanned: 1,
                updated: 1,
                failed: 0,
            }),
        )?;
        let third = row(&realm, "favicon")?;
        assert_eq!(third.runs, 3);
        assert_eq!(third.failures, 1);
        assert_eq!(third.items_updated, 3);
        assert_eq!(third.last_failed_items, 0);
        assert_eq!(third.last_error, None);
        Ok(())
    }

    #[tokio::test]
    async fn fetcher_rejects_non_http_urls() -> Result<()> {
        let http = HttpFetcher::new(&ScrapeConfig::default())?;
        let error = http
            .get(&Url::parse("file:///etc/passwd")?)
            .await
            .expect_err("file URLs are refused");
        assert!(error.to_string().contains("non-HTTP"));
        Ok(())
    }
}
