//! Continuous ingestion of the local auditd log.
//!
//! [`AuditdService`] tails `/var/log/audit/audit.log`, runs every record
//! through [`crate::rules`], persists the matches, and raises notifications
//! for the ones whose verdict asks for it. It is a
//! [`Continuous`](ServiceSchedule::Continuous) service: one `run` occupies the
//! service's whole lifetime and must react to `cancel`.

use crate::rules::Verdict;
use crate::{AuditEventData, AuditManagerData};
use anyhow::Result;
use chrono::{TimeDelta, Utc};
use linux_audit_parser::{EventID, Parser};
use sandpolis_instance::database::{RealmDatabase, Resident, ResidentVec};
use sandpolis_instance::notification::{Notification, notify};
use sandpolis_instance::service::{Service, ServiceReport, ServiceSchedule};
use sandpolis_instance::{InstanceId, LayerName};
use std::collections::HashMap;
use std::io::SeekFrom;
use std::path::PathBuf;
use std::time::{Duration, Instant};
use tokio::fs::File;
use tokio::io::{AsyncReadExt, AsyncSeekExt};
use tokio_util::sync::CancellationToken;
use tracing::{debug, trace, warn};

/// Where auditd writes its log by default.
const AUDIT_LOG_PATH: &str = "/var/log/audit/audit.log";

/// How often the tail looks for newly appended lines.
const POLL_INTERVAL: Duration = Duration::from_secs(1);

/// How long to wait before re-trying a log file that can't be opened, e.g.
/// because auditd isn't installed.
const RETRY_INTERVAL: Duration = Duration::from_secs(30);

/// At most one notification per record type in this window; the rest are
/// counted and folded into the next one.
const NOTIFY_WINDOW: Duration = Duration::from_secs(60);

/// How long an event this instance ingested is kept before it's trimmed.
const RETENTION_DAYS: i64 = 30;

/// Hard cap on stored events, so a hostile flood can't balloon the database.
const MAX_EVENTS: usize = 10_000;

#[derive(Clone)]
pub struct AuditdService {
    data: Resident<AuditManagerData>,
    events: ResidentVec<AuditEventData>,
    instance_id: InstanceId,
    path: PathBuf,
}

/// Per-record-type notification rate limiting.
#[derive(Default)]
struct Throttle(HashMap<u32, (Instant, u64)>);

impl Throttle {
    /// `Some(suppressed)` when a notification may go out now, where
    /// `suppressed` is how many were held back since the last one.
    fn allow(&mut self, ty: u32) -> Option<u64> {
        let now = Instant::now();
        match self.0.get_mut(&ty) {
            Some((last, suppressed)) if now.duration_since(*last) < NOTIFY_WINDOW => {
                *suppressed += 1;
                None
            }
            Some((last, suppressed)) => {
                let held = *suppressed;
                *last = now;
                *suppressed = 0;
                Some(held)
            }
            None => {
                self.0.insert(ty, (now, 0));
                Some(0)
            }
        }
    }
}

impl AuditdService {
    pub fn new(db: RealmDatabase, instance_id: InstanceId) -> Result<Self> {
        Ok(Self {
            data: db.resident(())?,
            events: db.resident_vec(())?,
            instance_id,
            path: AUDIT_LOG_PATH.into(),
        })
    }

    /// Watch a different log file, for tests.
    pub fn with_path(mut self, path: impl Into<PathBuf>) -> Self {
        self.path = path.into();
        self
    }

    /// The persisted ingestion cursor, if any.
    fn cursor(&self) -> Option<EventID> {
        let data = self.data.read();
        match (data.last_event_timestamp, data.last_event_sequence) {
            (Some(timestamp), Some(sequence)) => Some(EventID {
                timestamp,
                sequence,
            }),
            _ => None,
        }
    }

    fn save_cursor(&self, cursor: EventID) {
        if let Err(e) = self.data.update(|d| {
            d.last_event_timestamp = Some(cursor.timestamp);
            d.last_event_sequence = Some(cursor.sequence);
            Ok(())
        }) {
            warn!(error = %e, "Failed to persist the audit ingestion cursor");
        }
    }

    /// Parse one log line and act on it. `Ok(true)` when an event was stored.
    fn ingest(
        &self,
        line: &[u8],
        cursor: &mut Option<EventID>,
        throttle: &mut Throttle,
    ) -> Result<bool> {
        let msg = match Parser::default().parse(line) {
            Ok(msg) => msg,
            Err(e) => {
                // Malformed lines are the log's problem, not ours to die over.
                trace!(error = %e, "Skipping an unparseable audit record");
                return Ok(false);
            }
        };

        // Already seen before a restart or log rotation
        if let Some(c) = *cursor
            && msg.id <= c
        {
            return Ok(false);
        }
        *cursor = Some(msg.id);

        let Some((event, verdict)) = crate::rules::to_event(&msg, line, self.instance_id) else {
            return Ok(false);
        };

        if verdict.notify
            && let Some(suppressed) = throttle.allow(msg.ty.0)
        {
            self.notify_event(&event, &verdict, suppressed);
        }

        self.events.push(event)?;
        Ok(true)
    }

    fn notify_event(&self, event: &AuditEventData, verdict: &Verdict, suppressed: u64) {
        let title = match &event.acct {
            Some(acct) => format!("{}: {}", verdict.label, acct),
            None => verdict.label.to_string(),
        };

        let mut parts = Vec::new();
        if let Some(origin) = event.addr.as_ref().or(event.hostname.as_ref()) {
            parts.push(format!("from {origin}"));
        }
        if let Some(exe) = &event.exe {
            parts.push(format!("via {exe}"));
        }
        if let Some(terminal) = &event.terminal {
            parts.push(format!("on {terminal}"));
        }
        if suppressed > 0 {
            parts.push(format!("({suppressed} similar in the last minute)"));
        }

        let mut notification =
            Notification::new(verdict.severity, "Audit", title).about(self.instance_id);
        if !parts.is_empty() {
            notification = notification.body(parts.join(" "));
        }
        notify(notification);
    }

    /// Drop events this instance ingested that are past retention, and the
    /// oldest ones beyond [`MAX_EVENTS`]. Only our own rows: a replicated row
    /// belongs to the instance that ingested it.
    fn trim(&self) {
        let cutoff = Utc::now() - TimeDelta::days(RETENTION_DAYS);
        let mut kept = Vec::new();

        for resident in self.events.iter() {
            let (id, creation) = {
                let data = resident.read();
                if data._instance_id != self.instance_id {
                    continue;
                }
                (data._id, data._creation.timestamp())
            };

            if creation < cutoff {
                // `ResidentVec::remove` deletes the row and leaves its own view
                // to catch up asynchronously, so a trim that runs again before
                // the watcher has is expected to fail on rows already deleted.
                if let Err(e) = self.events.remove(id) {
                    debug!(error = %e, "Failed to trim an expired audit event");
                }
            } else {
                kept.push((creation, id));
            }
        }

        if kept.len() > MAX_EVENTS {
            kept.sort_by_key(|(creation, _)| *creation);
            for (_, id) in kept.drain(..kept.len() - MAX_EVENTS) {
                if let Err(e) = self.events.remove(id) {
                    debug!(error = %e, "Failed to trim a surplus audit event");
                }
            }
        }
    }

    async fn open(&self) -> std::io::Result<(File, u64)> {
        let file = File::open(&self.path).await?;
        let inode = file_id(&file.metadata().await?);
        Ok((file, inode))
    }
}

/// A stable identity for rotation detection; falls back to length-only
/// detection where inodes don't exist.
#[cfg(unix)]
fn file_id(meta: &std::fs::Metadata) -> u64 {
    std::os::unix::fs::MetadataExt::ino(meta)
}

#[cfg(not(unix))]
fn file_id(_meta: &std::fs::Metadata) -> u64 {
    0
}

impl Service for AuditdService {
    fn name(&self) -> &'static str {
        "auditd"
    }

    fn layer(&self) -> LayerName {
        LayerName::from("Audit")
    }

    fn description(&self) -> &'static str {
        "Ingests auditd events and raises alerts"
    }

    fn schedule(&self) -> ServiceSchedule {
        ServiceSchedule::Continuous {
            restart_backoff: Duration::from_secs(30),
        }
    }

    async fn run(&self, cancel: CancellationToken) -> Result<ServiceReport> {
        let mut report = ServiceReport::default();
        let mut throttle = Throttle::default();
        let mut cursor = self.cursor();
        let mut warned = false;

        'open: loop {
            // Open the log, waiting patiently if auditd isn't there (yet)
            let (mut file, inode) = loop {
                match self.open().await {
                    Ok(opened) => break opened,
                    Err(e) => {
                        if !warned {
                            warn!(path = %self.path.display(), error = %e,
                                "Cannot read the audit log; will keep retrying");
                            warned = true;
                        }
                        tokio::select! {
                            _ = cancel.cancelled() => return Ok(report),
                            _ = tokio::time::sleep(RETRY_INTERVAL) => {}
                        }
                    }
                }
            };
            warned = false;

            // With no cursor there is no way to tell old records from new, so
            // start at the end rather than flooding the database with history.
            // With one, scan the whole file and let the cursor skip what's
            // already been seen.
            let mut pos = if cursor.is_none() {
                file.seek(SeekFrom::End(0)).await?
            } else {
                0
            };

            let mut carry: Vec<u8> = Vec::new();
            loop {
                tokio::select! {
                    _ = cancel.cancelled() => return Ok(report),
                    _ = tokio::time::sleep(POLL_INTERVAL) => {}
                }

                // Everything appended since the last pass
                let mut chunk = Vec::new();
                file.read_to_end(&mut chunk).await?;
                pos += chunk.len() as u64;

                if !chunk.is_empty() {
                    carry.extend_from_slice(&chunk);
                    while let Some(nl) = carry.iter().position(|&b| b == b'\n') {
                        // The parser wants the newline kept on
                        let line: Vec<u8> = carry.drain(..=nl).collect();
                        if line.len() == 1 {
                            continue;
                        }

                        report.scanned += 1;
                        match self.ingest(&line, &mut cursor, &mut throttle) {
                            Ok(true) => report.updated += 1,
                            Ok(false) => {}
                            Err(e) => {
                                warn!(error = %e, "Failed to store an audit event");
                                report.failed += 1;
                            }
                        }
                    }

                    if let Some(c) = cursor {
                        self.save_cursor(c);
                    }
                    self.trim();
                }

                // Rotation: the path now names a different file, or the file
                // was truncated under us. Reopen and rescan; the cursor keeps
                // old records from coming back.
                match tokio::fs::metadata(&self.path).await {
                    Ok(meta) if file_id(&meta) == inode && meta.len() >= pos => {}
                    _ => {
                        debug!(path = %self.path.display(), "Audit log rotated; reopening");
                        continue 'open;
                    }
                }
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use sandpolis_instance::database::DatabaseManager;
    use sandpolis_instance::realm::RealmName;
    use sandpolis_instance::{InstanceType, test_db};
    use std::io::Write;

    const FAILED_LOGIN: &str = "type=USER_LOGIN msg=audit(1723400000.123:456): pid=1234 uid=0 auid=1000 ses=3 msg='op=login acct=\"tyler\" exe=\"/usr/sbin/sshd\" addr=203.0.113.7 terminal=ssh res=failed'";
    const ADD_USER: &str = "type=ADD_USER msg=audit(1723400001.000:457): pid=999 uid=0 auid=1000 ses=2 msg='op=adding user id=1001 exe=\"/usr/sbin/useradd\" terminal=pts/0 res=success'";

    fn service(path: &std::path::Path) -> Result<AuditdService> {
        let database: DatabaseManager = test_db!(AuditEventData, AuditManagerData);
        Ok(AuditdService::new(
            database.realm(RealmName::default())?,
            InstanceId::new(InstanceType::Agent),
        )?
        .with_path(path))
    }

    /// Poll until the service's view holds `count` events, or give up.
    async fn wait_for_events(service: &AuditdService, count: usize) {
        for _ in 0..100 {
            if service.events.len() >= count {
                return;
            }
            tokio::time::sleep(Duration::from_millis(100)).await;
        }
    }

    #[test_log::test(tokio::test)]
    async fn tails_appended_events_across_rotation() -> Result<()> {
        let dir = tempfile::tempdir()?;
        let path = dir.path().join("audit.log");
        std::fs::write(&path, "")?;

        let service = service(&path)?;
        let cancel = CancellationToken::new();
        let task = tokio::spawn({
            let service = service.clone();
            let cancel = cancel.clone();
            async move { service.run(cancel).await }
        });

        // Give the tail time to open the empty file and take up position
        tokio::time::sleep(Duration::from_millis(500)).await;

        let mut log = std::fs::OpenOptions::new().append(true).open(&path)?;
        writeln!(log, "{FAILED_LOGIN}")?;
        log.flush()?;

        wait_for_events(&service, 1).await;
        assert_eq!(service.events.len(), 1);
        let stored = service.events.iter().next().unwrap();
        assert_eq!(stored.read().record_type, "USER_LOGIN");
        assert_eq!(stored.read().acct.as_deref(), Some("tyler"));

        // Rotate: the old file moves away and a fresh one appears
        std::fs::rename(&path, dir.path().join("audit.log.1"))?;
        std::fs::write(&path, format!("{ADD_USER}\n"))?;

        wait_for_events(&service, 2).await;
        assert_eq!(service.events.len(), 2);

        // The cursor survived it all
        assert_eq!(service.cursor().unwrap().sequence, 457);

        cancel.cancel();
        task.await??;
        Ok(())
    }

    #[test_log::test(tokio::test)]
    async fn first_run_skips_history() -> Result<()> {
        let dir = tempfile::tempdir()?;
        let path = dir.path().join("audit.log");

        // History that predates the agent must not flood the database
        std::fs::write(&path, format!("{FAILED_LOGIN}\n"))?;

        let service = service(&path)?;
        let cancel = CancellationToken::new();
        let task = tokio::spawn({
            let service = service.clone();
            let cancel = cancel.clone();
            async move { service.run(cancel).await }
        });

        tokio::time::sleep(Duration::from_millis(1500)).await;
        assert_eq!(service.events.len(), 0);

        // But new arrivals are picked up
        let mut log = std::fs::OpenOptions::new().append(true).open(&path)?;
        writeln!(log, "{ADD_USER}")?;
        log.flush()?;

        wait_for_events(&service, 1).await;
        assert_eq!(service.events.len(), 1);

        cancel.cancel();
        task.await??;
        Ok(())
    }

    #[test_log::test(tokio::test)]
    #[ignore = "requires a running auditd"]
    async fn reads_the_real_audit_log() -> Result<()> {
        let database: DatabaseManager = test_db!(AuditEventData, AuditManagerData);
        let service = AuditdService::new(
            database.realm(RealmName::default())?,
            InstanceId::new(InstanceType::Agent),
        )?;

        let cancel = CancellationToken::new();
        let task = tokio::spawn({
            let service = service.clone();
            let cancel = cancel.clone();
            async move { service.run(cancel).await }
        });

        tokio::time::sleep(Duration::from_secs(5)).await;
        cancel.cancel();
        let report = task.await??;
        println!("scanned {} audit records", report.scanned);
        Ok(())
    }
}
