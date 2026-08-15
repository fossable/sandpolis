//! Deciding, and recording, whether the instances attached to this server are up.
//!
//! The connections themselves are the evidence: an instance with a live socket
//! here is reachable, and one without is a question. How that question is
//! answered depends on how the instance was told to connect. A continuously
//! connected agent is offline the moment its socket dies. A polling agent is
//! *expected* to be gone between check-ins, so it is only offline once it misses
//! one — which is why it announces its schedule on the way in
//! ([`PollAnnouncement`]).
//!
//! The conclusion is written to
//! [`LivenessData`](sandpolis_instance::network::liveness::LivenessData), which
//! replicates, so a client learns about agents it has no connection to.

use anyhow::Result;
use chrono::{DateTime, TimeDelta, Utc};
use cron::Schedule;
use sandpolis_instance::network::NetworkManager;
use sandpolis_instance::network::liveness::{LivenessData, MAX_SCHEDULE_LEN, PollAnnouncement};
use sandpolis_instance::notification::{Notification, notify};
use sandpolis_instance::database::ResidentVec;
use sandpolis_instance::{InstanceId, InstanceType};
use std::collections::BTreeMap;
use std::str::FromStr;
use std::sync::Arc;
use tokio::sync::Notify;
use tokio::time::{Duration, sleep};
use tracing::{debug, info, warn};

/// How long to wait after a connection change before acting, so a burst of
/// reconnects produces one pass.
const DEBOUNCE: Duration = Duration::from_millis(500);

/// How often to re-check with nothing having happened. Unlike the ownership
/// reconcilers this tick is load-bearing rather than a safety net: a check-in
/// deadline passes with no event to wake on.
const SWEEP_INTERVAL: Duration = Duration::from_secs(30);

/// Slack on top of a polling agent's window before a missed check-in counts.
/// Covers clock skew and a slow start, so a healthy agent is never called dead.
const POLL_GRACE: TimeDelta = TimeDelta::minutes(2);

/// The furthest ahead a check-in deadline may be set. A schedule arrives from
/// the peer, so without this a hostile or fat-fingered one ("once a year") would
/// leave a node stuck at "online" indefinitely.
const MAX_POLL_HORIZON: TimeDelta = TimeDelta::days(7);

/// This server's opinion of who is up.
pub struct Liveness {
    rows: ResidentVec<LivenessData>,

    /// The instance whose opinions these are. Only rows scoped to it are ours to
    /// write — another server's row is a replica, and writing it would fork a
    /// record that has exactly one writer by construction.
    observer: InstanceId,
}

impl Liveness {
    pub fn new(network: &NetworkManager, observer: InstanceId) -> Self {
        Self {
            rows: network.liveness.clone(),
            observer,
        }
    }

    /// Record that `subject` is attached, notifying if this server has never
    /// seen it before.
    fn mark_online(&self, subject: InstanceId, poll: Option<&PollAnnouncement>) -> Result<()> {
        let now = Utc::now();
        let expected_next = poll.and_then(|poll| next_checkin(poll, now));

        match sandpolis_instance::network::liveness::find(&self.rows, self.observer, subject) {
            Some(row) => {
                let was_online = row.read().online;
                row.update(|d| {
                    d.online = true;
                    d.last_seen = now;
                    d.poll_schedule = poll.map(|p| p.schedule.clone());
                    d.expected_next = expected_next;
                    Ok(())
                })?;
                if !was_online {
                    info!(instance = %subject, "Instance came back online");
                }
            }
            None => {
                self.rows.push(LivenessData {
                    subject,
                    online: true,
                    last_seen: now,
                    poll_schedule: poll.map(|p| p.schedule.clone()),
                    expected_next,
                    _instance_id: self.observer,
                    ..Default::default()
                })?;

                // No row at all means this server has never seen it attach —
                // which is what "joined for the first time" means from here.
                info!(instance = %subject, "Instance joined for the first time");
                notify(
                    Notification::info(
                        LAYER,
                        format!("New {} joined", describe(subject).to_lowercase()),
                    )
                        .body(subject.to_string())
                        .about(subject),
                );
            }
        }

        Ok(())
    }

    /// Record that `subject` is no longer reachable.
    fn mark_offline(&self, subject: InstanceId) -> Result<()> {
        let Some(row) = sandpolis_instance::network::liveness::find(&self.rows, self.observer, subject)
        else {
            return Ok(());
        };

        row.update(|d| {
            d.online = false;
            Ok(())
        })?;

        warn!(instance = %subject, "Instance went offline");
        notify(offline_notification(subject));
        Ok(())
    }

    /// Bring every row this server owns in line with `attached`, the instances
    /// it currently holds a live connection to.
    fn reconcile(&self, attached: &BTreeMap<InstanceId, Option<PollAnnouncement>>, now: DateTime<Utc>) {
        for (subject, poll) in attached {
            // Write on the edge into online, and on every check-in by a polling
            // peer — that is what moves its next deadline forward. Rewriting an
            // unchanged row on every sweep would otherwise replicate a record
            // per instance per sweep, forever.
            let write = match sandpolis_instance::network::liveness::find(
                &self.rows,
                self.observer,
                *subject,
            ) {
                Some(row) => !row.read().online || poll.is_some(),
                None => true,
            };

            if write && let Err(e) = self.mark_online(*subject, poll.as_ref()) {
                warn!(error = %e, instance = %subject, "Failed to record an instance as online");
            }
        }

        for subject in self.overdue(attached, now) {
            if let Err(e) = self.mark_offline(subject) {
                warn!(error = %e, instance = %subject, "Failed to record an instance as offline");
            }
        }
    }

    /// The instances this server calls online that are no longer attached and
    /// are out of time to come back.
    fn overdue(
        &self,
        attached: &BTreeMap<InstanceId, Option<PollAnnouncement>>,
        now: DateTime<Utc>,
    ) -> Vec<InstanceId> {
        self.rows
            .iter()
            .filter_map(|row| {
                let row = row.read();
                if row._instance_id != self.observer || !row.online {
                    return None;
                }
                if attached.contains_key(&row.subject) {
                    return None;
                }

                // A continuous instance has no deadline to wait for: the
                // connection was the signal, and it's gone. A polling one is
                // supposed to be away, right up until it misses a window.
                match row.expected_next {
                    None => Some(row.subject),
                    Some(deadline) => (now > deadline).then_some(row.subject),
                }
            })
            .collect()
    }
}

/// The layer these notifications are attributed to.
const LAYER: &str = "Network";

/// What to call an instance in a notification.
fn describe(subject: InstanceId) -> &'static str {
    match subject.instance_type() {
        Some(InstanceType::Agent) => "Agent",
        Some(InstanceType::Server) => "Server",
        Some(InstanceType::Client) => "Client",
        None => "Instance",
    }
}

/// When a polling peer that just checked in is next due, with slack.
///
/// `None` when the schedule doesn't parse or points implausibly far ahead —
/// better to treat the peer as continuous (offline as soon as it drops) than to
/// let an unusable schedule keep a dead node lit up.
fn next_checkin(poll: &PollAnnouncement, from: DateTime<Utc>) -> Option<DateTime<Utc>> {
    if poll.schedule.len() > MAX_SCHEDULE_LEN {
        return None;
    }

    let schedule = Schedule::from_str(&poll.schedule).ok()?;
    let next = schedule.after(&from).next()?;
    let deadline = next + TimeDelta::from_std(poll.timeout).unwrap_or(TimeDelta::zero()) + POLL_GRACE;

    (deadline <= from + MAX_POLL_HORIZON).then_some(deadline)
}

/// The instances attached to this server right now, with whatever check-in
/// schedule they announced.
///
/// Unlike [`ownership::attached_instances`](crate::ownership::attached_instances)
/// this keeps server peers: a local stratum server going quiet is exactly the
/// kind of thing the global stratum server should notice. Clients are dropped —
/// someone opening the GUI is not an event worth a notification.
fn attached(network: &NetworkManager) -> BTreeMap<InstanceId, Option<PollAnnouncement>> {
    network
        .live_inbound()
        .iter()
        .map(|c| (c.data.read().remote_instance, c.poll.get().cloned()))
        .filter(|(id, _)| !id.is_client())
        .collect()
}

/// Keep this server's liveness rows current.
///
/// Wakes on connection changes (a socket opening, or a janitor removing the row
/// of one that died) and on a timer, since a missed check-in deadline passes
/// with nothing to wake on.
pub async fn maintain_liveness(network: NetworkManager, liveness: Arc<Liveness>) {
    let notify = Arc::new(Notify::new());
    {
        let notify = notify.clone();
        network.connections.listen(move |_| notify.notify_one());
    }

    loop {
        liveness.reconcile(&attached(&network), Utc::now());

        tokio::select! {
            _ = notify.notified() => sleep(DEBOUNCE).await,
            _ = sleep(SWEEP_INTERVAL) => {}
        }
    }
}

/// Record that a server this instance dialed is no longer reachable.
///
/// The peer at the other end of a link we opened is the one case the reconciler
/// above cannot cover: an unreachable server writes nothing, so somebody on this
/// side has to say it.
pub fn report_unreachable(peer: InstanceId) {
    debug!(instance = %peer, "Reporting an unreachable peer");
    notify(offline_notification(peer));
}

/// The notification raised whenever an instance stops being reachable, however
/// this side found out. Shared so the reconciler and the dialing side can't
/// drift into wording the same event two ways.
fn offline_notification(subject: InstanceId) -> Notification {
    Notification::warn(LAYER, format!("{} went offline", describe(subject)))
        .body(subject.to_string())
        .about(subject)
}

#[cfg(test)]
mod test_liveness {
    use super::*;

    fn polling(schedule: &str) -> PollAnnouncement {
        PollAnnouncement {
            schedule: schedule.into(),
            timeout: Duration::from_secs(30),
        }
    }

    /// A five-minute schedule with a 30s window puts the deadline at the next
    /// tick plus the window plus the grace, not at the next tick.
    #[test]
    fn deadline_covers_the_window_and_the_grace() {
        let now = DateTime::parse_from_rfc3339("2026-08-14T12:00:00Z")
            .unwrap()
            .with_timezone(&Utc);

        let deadline = next_checkin(&polling("0 */5 * * * *"), now).unwrap();

        assert_eq!(
            deadline,
            DateTime::parse_from_rfc3339("2026-08-14T12:05:30Z")
                .unwrap()
                .with_timezone(&Utc)
                + POLL_GRACE
        );
    }

    #[test]
    fn an_unparseable_schedule_yields_no_deadline() {
        let now = Utc::now();
        assert!(next_checkin(&polling("not a schedule"), now).is_none());
        assert!(next_checkin(&polling(&"0 ".repeat(MAX_SCHEDULE_LEN)), now).is_none());
    }

    /// A schedule far enough out would otherwise leave a dead node lit up until
    /// it came around.
    #[test]
    fn an_implausible_schedule_yields_no_deadline() {
        let now = Utc::now();
        // Once a year, on the first of January.
        assert!(next_checkin(&polling("0 0 0 1 1 * *"), now).is_none());
    }
}
