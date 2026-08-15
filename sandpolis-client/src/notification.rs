//! Client-side surfacing of notifications raised anywhere in the estate.
//!
//! Every instance that raises a notification writes a
//! [`NotificationData`] row, which the sync engine replicates here. This module
//! watches the client's local copy and decides how the user finds out:
//!
//! - looking at the window → an in-app toast, sent to the GUI over
//!   [`set_toast_sink`]
//! - not looking, or no GUI at all (a TUI subcommand, a headless run) → the
//!   operating system's own notification interface
//!
//! It reacts to a database change callback rather than polling, unlike the rest
//! of the client's views: a `bind_text` closure runs every frame, which is fine
//! for a panel that is open but wrong for something that must fire exactly once
//! per notification.

use anyhow::Result;
use chrono::Utc;
use native_db::*;
use native_model::Model;
use sandpolis_instance::database::{
    DataIdentifier, DatabaseManager, Resident, ResidentVecEvent,
};
use sandpolis_instance::notification::{NotificationData, notification_model_id};
use sandpolis_instance::realm::RealmName;
use sandpolis_macros::data;
use std::collections::BTreeSet;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{Mutex, OnceLock};
use tokio::sync::mpsc::UnboundedSender;

/// The creation time of the newest notification this client has surfaced, as
/// milliseconds since the Unix epoch. Carries across restarts so a fresh
/// subscription's snapshot isn't announced all over again.
///
/// Client-local: it has no [`SyncRegistration`](sandpolis_instance::database::sync::SyncRegistration),
/// so it never replicates — "have I shown this to the user" is a fact about
/// this installation, not about the estate.
#[data]
#[derive(Default)]
pub struct NotificationWatermarkData {
    pub last_seen: i64,
}

/// What this client has already shown the user.
///
/// Two mechanisms, because neither covers the whole problem on its own:
///
/// - `session_start` is the persisted watermark as of startup. Anything created
///   before it belongs to a previous run and must not be announced again, which
///   is what keeps a subscription's opening snapshot quiet.
/// - `seen` is exact, and handles repeats *within* a run — a reconnect replays
///   the snapshot, and `_creation` only has millisecond resolution, so two
///   notifications can easily share a timestamp.
struct Surfaced {
    watermark: Resident<NotificationWatermarkData>,
    session_start: i64,
    seen: Mutex<BTreeSet<DataIdentifier>>,
}

static SURFACED: OnceLock<Surfaced> = OnceLock::new();
static TOAST_SINK: OnceLock<UnboundedSender<NotificationData>> = OnceLock::new();

/// Whether the user is currently looking at the client.
///
/// Defaults to `false` so a client with no GUI — a TUI subcommand, a `--json`
/// run — goes straight to native notifications.
static FOREGROUND: AtomicBool = AtomicBool::new(false);

/// The sync `model_id` for notifications.
pub fn model_id() -> u32 {
    notification_model_id()
}

/// Subscribe to notifications from every instance.
///
/// Unscoped on purpose: the point is to hear about anything that happens
/// anywhere, not only the instance whose panel happens to be open. Notifications
/// are few and their rows are small.
pub fn subscribe() {
    crate::sync::subscribe(model_id(), None);
}

/// Drop the subscription created by [`subscribe`].
pub fn unsubscribe() {
    crate::sync::unsubscribe(model_id(), None);
}

/// Where in-app toasts go. Registered by the GUI; without it every notification
/// is delivered natively.
pub fn set_toast_sink(sender: UnboundedSender<NotificationData>) {
    let _ = TOAST_SINK.set(sender);
}

/// Record whether the client currently has the user's attention.
pub fn set_foreground(foreground: bool) {
    FOREGROUND.store(foreground, Ordering::Relaxed);
}

/// Start surfacing notifications. Call once, at client startup.
///
/// Watches the collection the process notifier already maintains, so this adds
/// a callback rather than a second view of the same rows.
pub fn watch(database: &DatabaseManager) -> Result<()> {
    let realm = database.realm(RealmName::default())?;
    let watermark: Resident<NotificationWatermarkData> = realm.resident(())?;

    // A client that has never run starts at "now". Otherwise its first
    // subscription — which streams a full snapshot before any live records —
    // would announce every notification in the estate's retention window at
    // once.
    if watermark.read().last_seen == 0 {
        let now = Utc::now().timestamp_millis();
        watermark.update(|w| {
            w.last_seen = now;
            Ok(())
        })?;
    }

    let session_start = watermark.read().last_seen;
    let _ = SURFACED.set(Surfaced {
        watermark,
        session_start,
        seen: Mutex::new(BTreeSet::new()),
    });

    let Some(notifier) = sandpolis_instance::notification::handle() else {
        anyhow::bail!("The notifier is not installed");
    };

    notifier.data().listen(move |event| {
        // Only new rows. Replication also reports updates as records settle,
        // and re-announcing those would double-notify.
        let ResidentVecEvent::Added(resident) = event else {
            return;
        };

        let notification = resident.read().clone();
        if !claim(&notification) {
            return;
        }

        deliver(notification);
    });

    Ok(())
}

/// Whether this notification is ours to surface, recording it if so.
fn claim(notification: &NotificationData) -> bool {
    let Some(surfaced) = SURFACED.get() else {
        return false;
    };

    // Strictly older than this run's starting point means a previous run
    // already dealt with it. Rows *at* that instant are let through and left to
    // the id check, since a notification raised in the same millisecond the
    // client started is new, not history.
    let created = notification._creation.timestamp().timestamp_millis();
    if created < surfaced.session_start {
        return false;
    }

    if !surfaced
        .seen
        .lock()
        .unwrap()
        .insert(notification._id)
    {
        return false;
    }

    // Persist how far this run got, so the next one starts past it.
    if let Err(e) = surfaced.watermark.update(|w| {
        w.last_seen = w.last_seen.max(created);
        Ok(())
    }) {
        tracing::debug!(error = %e, "Failed to advance the notification watermark");
    }
    true
}

/// Show one notification, by whichever route currently fits.
fn deliver(notification: NotificationData) {
    let sink = TOAST_SINK.get().filter(|_| FOREGROUND.load(Ordering::Relaxed));

    match sink {
        // A dead channel means the GUI is gone; fall back rather than drop it.
        Some(sink) => {
            if let Err(e) = sink.send(notification) {
                sandpolis_instance::notification::native::deliver(&e.0);
            }
        }
        None => sandpolis_instance::notification::native::deliver(&notification),
    }
}
