//! Whether an instance is reachable, as observed by the server it attaches to.
//!
//! Connections themselves ([`ConnectionData`](super::ConnectionData)) are local
//! bookkeeping that never leaves the process holding the socket, so nobody else
//! can tell from them whether an agent is up. Liveness is the replicated
//! conclusion drawn from them: the server an instance attaches to writes a row
//! saying so, and that row travels the ordinary path — up to the global stratum
//! server, then out to any client subscribed to the model.
//!
//! A row is scoped by the *observer* (`_instance_id`) and names what it is about
//! in [`LivenessData::subject`], the same split
//! [`NotificationData`](crate::notification::NotificationData) uses. Scoping it
//! by the subject instead would put it in the scope a server pulls *from* that
//! instance, and would make it unwritable exactly when it matters most — a
//! freshly restarted edge server, still waiting on a grant, watching the agent
//! in front of it.
//!
//! Because an observer's row can only be as current as the observer, a reader
//! resolves the estate with [`reachable`] rather than trusting rows outright.

use crate::InstanceId;
use crate::database::ResidentVec;
use chrono::{DateTime, Utc};
use native_db::*;
use native_model::Model;
use sandpolis_macros::data;
use serde_with::chrono::serde::{ts_seconds, ts_seconds_option};
use std::collections::BTreeSet;

/// What a polling peer said about its own check-in schedule when it connected.
///
/// Kept as the raw cron string: the peer is the one that has to honor it, and
/// only the server that reasons about missed check-ins needs to parse it.
#[derive(Clone, Debug, PartialEq, Eq)]
pub struct PollAnnouncement {
    pub schedule: String,
    pub timeout: std::time::Duration,
}

/// The longest cron schedule string worth looking at. A schedule arrives as an
/// untrusted header, and anything sane is far shorter than this.
pub const MAX_SCHEDULE_LEN: usize = 128;

/// One server's view of one instance's reachability.
#[data(instance)]
#[derive(Default)]
pub struct LivenessData {
    /// The instance this row is about. Deliberately not unique: several servers
    /// may each hold an opinion about the same instance.
    #[secondary_key]
    pub subject: InstanceId,

    pub online: bool,

    /// When the observer last had a live connection to the subject.
    #[serde(with = "ts_seconds")]
    pub last_seen: DateTime<Utc>,

    /// The cron schedule a polling agent reported when it last checked in.
    /// `None` means the subject stays connected continuously, so losing the
    /// connection is immediately a fault.
    pub poll_schedule: Option<String>,

    /// When the next check-in is due, for a polling subject that is currently
    /// disconnected. Passing this without a connection is what makes it offline.
    #[serde(with = "ts_seconds_option")]
    pub expected_next: Option<DateTime<Utc>>,
}

// Scoped by the observing server so its rows replicate as part of that server's
// own data, exactly like the notifications it raises.
inventory::submit! {
    crate::database::sync::SyncRegistration(
        |r| r.register_scoped::<LivenessData>(|d| d._instance_id))
}

/// The sync `model_id` for [`LivenessData`], used by clients to subscribe.
pub fn liveness_model_id() -> u32 {
    <LivenessData as Model>::native_model_id()
}

/// Every instance that is currently reachable, according to `rows`.
///
/// An observer's opinion only counts while the observer is itself reachable.
/// Otherwise a server that was killed outright would freeze every agent it knew
/// at "online" forever, since a dead process writes no correction. `direct` is
/// what the caller knows first-hand — the peers it holds a live connection to —
/// and the rest of the estate is resolved outward from there.
pub fn reachable(
    rows: impl IntoIterator<Item = LivenessData>,
    direct: impl IntoIterator<Item = InstanceId>,
) -> BTreeSet<InstanceId> {
    let rows: Vec<LivenessData> = rows.into_iter().filter(|row| row.online).collect();
    let mut online: BTreeSet<InstanceId> = direct.into_iter().collect();

    // Each pass trusts one more hop out (client -> GS -> LS -> agent), so this
    // settles in as many passes as the network is deep.
    loop {
        let mut grew = false;
        for row in &rows {
            if online.contains(&row._instance_id) && online.insert(row.subject) {
                grew = true;
            }
        }
        if !grew {
            return online;
        }
    }
}

/// The rows in `data` that `observer` wrote about `subject`, if any.
///
/// A server only ever edits its own opinion — someone else's row is not ours to
/// write, and doing so would fork a replicated record.
pub fn find(
    data: &ResidentVec<LivenessData>,
    observer: InstanceId,
    subject: InstanceId,
) -> Option<crate::database::Resident<LivenessData>> {
    data.iter().find(|row| {
        let row = row.read();
        row._instance_id == observer && row.subject == subject
    })
}

#[cfg(test)]
mod test_reachable {
    use super::*;
    use crate::InstanceType;

    fn row(observer: InstanceId, subject: InstanceId, online: bool) -> LivenessData {
        LivenessData {
            subject,
            online,
            _instance_id: observer,
            ..Default::default()
        }
    }

    #[test]
    fn resolves_outward_from_direct_connections() {
        let gs = InstanceId::new(InstanceType::Server);
        let ls = InstanceId::new(InstanceType::Server);
        let agent = InstanceId::new(InstanceType::Agent);

        // The client only reaches the GS directly; the GS vouches for the LS,
        // which vouches for the agent.
        let rows = vec![row(gs, ls, true), row(ls, agent, true)];
        let online = reachable(rows, [gs]);

        assert!(online.contains(&ls));
        assert!(online.contains(&agent));
    }

    /// The case a killed server would otherwise get wrong: its rows still say
    /// its agents are up, because it never got to say otherwise.
    #[test]
    fn a_dead_observer_vouches_for_nobody() {
        let gs = InstanceId::new(InstanceType::Server);
        let ls = InstanceId::new(InstanceType::Server);
        let agent = InstanceId::new(InstanceType::Agent);

        let rows = vec![row(gs, ls, false), row(ls, agent, true)];
        let online = reachable(rows, [gs]);

        assert!(!online.contains(&ls));
        assert!(!online.contains(&agent));
    }

    #[test]
    fn an_offline_row_is_not_a_vouch() {
        let gs = InstanceId::new(InstanceType::Server);
        let agent = InstanceId::new(InstanceType::Agent);

        let online = reachable(vec![row(gs, agent, false)], [gs]);
        assert!(!online.contains(&agent));
    }
}
