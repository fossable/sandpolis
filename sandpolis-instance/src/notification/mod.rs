//! User-facing notifications that any subsystem can raise.
//!
//! A subsystem calls [`notify`] with a [`Notification`]; the process notifier turns
//! it into a [`NotificationData`] row owned by this instance. From there the
//! ordinary replication path takes over — an agent's notification reaches its
//! owning server, then the global stratum server, then any client subscribed to
//! the model — so nothing here needs a stream or a protocol of its own.
//!
//! The client end is what actually shows something to a person: it watches the
//! model and either raises an in-app toast or hands the notification to the
//! operating system through [`native`], depending on whether the user is looking
//! at the window.
//!
//! # Raising a notification
//!
//! ```ignore
//! notification::notify(
//!     Notification::error("Health", format!("{name} failed"))
//!         .body("The unit entered the failed state")
//!         .about(instance_id),
//! );
//! ```
//!
//! [`notify`] never returns an error. Call sites are collectors and detection
//! loops where a failed notification is not worth propagating — it is logged and
//! dropped, because losing a notification must never take down the work that
//! raised it.

use crate::InstanceId;
use crate::LayerName;
use crate::database::{RealmDatabase, ResidentVec};
use anyhow::Result;
use chrono::{TimeDelta, Utc};
use native_db::*;
use native_model::Model;
use sandpolis_macros::data;
use serde::{Deserialize, Serialize};
use std::sync::OnceLock;
use strum::{Display, EnumString};
use tracing::{debug, warn};

#[cfg(feature = "client")]
pub mod native;

/// How long a notification this instance raised is kept before it's trimmed.
const RETENTION_DAYS: i64 = 7;

/// How loudly a notification asks for the user's attention.
#[derive(
    Serialize, Deserialize, Clone, Copy, Debug, Default, PartialEq, Eq, Display, EnumString,
)]
#[strum(serialize_all = "kebab-case")]
pub enum Severity {
    /// Something happened that the user may want to know about.
    #[default]
    Info,
    /// Something is wrong but still working, or is about to be wrong.
    Warn,
    /// Something is broken and needs attention.
    Error,
}

impl Severity {
    /// A stable ordinal, for passing severity across an FFI boundary.
    pub fn rank(&self) -> i32 {
        match self {
            Self::Info => 0,
            Self::Warn => 1,
            Self::Error => 2,
        }
    }
}

/// One notification, as stored and replicated.
///
/// `_instance_id` is the instance that *raised* it, because that decides the
/// row's write scope. What the notification is *about* is [`subject`], which is
/// usually the same instance but differs when, say, a server reports on one of
/// its agents.
#[data(instance, defaults)]
pub struct NotificationData {
    /// The layer that raised it, so a client can attribute and filter.
    #[secondary_key]
    pub layer: String,

    pub severity: Severity,

    /// One line. This is what a native notification shows as its summary, so it
    /// should read on its own.
    pub title: String,

    /// Optional detail, shown under the title.
    pub body: Option<String>,

    /// The instance this is about, when that isn't the instance that raised it.
    pub subject: Option<InstanceId>,
}

// Scoped by the raising instance so an agent's notifications replicate up to
// whichever server owns it, exactly like the rest of that instance's data.
inventory::submit! {
    crate::database::sync::SyncRegistration(
        |r| r.register_scoped::<NotificationData>(|d| d._instance_id))
}

/// The sync `model_id` for [`NotificationData`], used by clients to subscribe.
pub fn notification_model_id() -> u32 {
    <NotificationData as Model>::native_model_id()
}

/// A notification under construction.
pub struct Notification {
    layer: LayerName,
    severity: Severity,
    title: String,
    body: Option<String>,
    subject: Option<InstanceId>,
}

impl Notification {
    pub fn new(
        severity: Severity,
        layer: impl Into<LayerName>,
        title: impl Into<String>,
    ) -> Self {
        Self {
            layer: layer.into(),
            severity,
            title: title.into(),
            body: None,
            subject: None,
        }
    }

    pub fn info(layer: impl Into<LayerName>, title: impl Into<String>) -> Self {
        Self::new(Severity::Info, layer, title)
    }

    pub fn warn(layer: impl Into<LayerName>, title: impl Into<String>) -> Self {
        Self::new(Severity::Warn, layer, title)
    }

    pub fn error(layer: impl Into<LayerName>, title: impl Into<String>) -> Self {
        Self::new(Severity::Error, layer, title)
    }

    /// Add detail shown under the title.
    pub fn body(mut self, body: impl Into<String>) -> Self {
        self.body = Some(body.into());
        self
    }

    /// Name the instance this notification is about, when it isn't the one
    /// raising it.
    pub fn about(mut self, subject: InstanceId) -> Self {
        self.subject = Some(subject);
        self
    }
}

/// The process notifier. Installed once by
/// [`InstanceManager::new`](crate::InstanceManager::new), which is the first thing
/// that knows both the database and this instance's id.
static NOTIFIER: OnceLock<Notifier> = OnceLock::new();

/// Owns the notification collection for this process.
pub struct Notifier {
    data: ResidentVec<NotificationData>,
    instance_id: InstanceId,
}

impl Notifier {
    /// Every notification in this instance's database — the ones it raised plus,
    /// on a client or server, everything replicated in.
    ///
    /// A client watches this with
    /// [`ResidentVec::listen`](crate::database::ResidentVec::listen) rather than
    /// polling.
    pub fn data(&self) -> &ResidentVec<NotificationData> {
        &self.data
    }

    fn raise(&self, notification: Notification) -> Result<()> {
        self.trim();

        // The bookkeeping fields are spelled out rather than filled from
        // `Default::default()`, which would build a throwaway `InstanceId` —
        // and that panics on a build with no instance type compiled in.
        self.data.push(NotificationData {
            layer: notification.layer.0,
            severity: notification.severity,
            title: notification.title,
            body: notification.body,
            subject: notification.subject,
            _instance_id: self.instance_id,
            _id: Default::default(),
            _revision: Default::default(),
            _creation: Default::default(),
        })?;
        Ok(())
    }

    /// Drop notifications this instance raised more than [`RETENTION_DAYS`] ago.
    ///
    /// Only our own: a replicated row belongs to the instance that raised it,
    /// which trims its own and replicates the removal. Deleting one here would
    /// be a write outside our scope anyway.
    fn trim(&self) {
        let cutoff = Utc::now() - TimeDelta::days(RETENTION_DAYS);
        for resident in self.data.iter() {
            let expired = {
                let data = resident.read();
                (data._instance_id == self.instance_id && data._creation.timestamp() < cutoff)
                    .then_some(data._id)
            };

            // `ResidentVec::remove` deletes the row and leaves its own view to
            // catch up asynchronously, so a trim that runs again before the
            // watcher has is expected to fail on the rows it already deleted.
            if let Some(id) = expired
                && let Err(e) = self.data.remove(id)
            {
                debug!(error = %e, "Failed to trim an expired notification");
            }
        }
    }
}

/// Install the process notifier. Idempotent — startup builds an `InstanceManager`
/// more than once, and only the first one takes effect.
pub fn install(realm: &RealmDatabase, instance_id: InstanceId) {
    if NOTIFIER.get().is_some() {
        return;
    }

    match realm.resident_vec(()) {
        Ok(data) => {
            let _ = NOTIFIER.set(Notifier { data, instance_id });
        }
        Err(e) => warn!(error = %e, "Failed to install the notifier; notifications will be dropped"),
    }
}

/// The process notifier, if it has been installed.
pub fn handle() -> Option<&'static Notifier> {
    NOTIFIER.get()
}

/// Raise a notification.
///
/// Fire-and-forget: a failure is logged rather than returned, so a call site in
/// the middle of a collector doesn't have to decide what to do about it.
pub fn notify(notification: Notification) {
    let Some(notifier) = NOTIFIER.get() else {
        warn!(
            title = %notification.title,
            "Dropping a notification because the notifier is not installed"
        );
        return;
    };

    if let Err(e) = notifier.raise(notification) {
        warn!(error = %e, "Failed to raise a notification");
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::id::AgentId;
    
    use crate::database::DataCreation;
    use crate::realm::RealmName;
    use crate::test_db;

    /// A throwaway instance for tests that just need one to exist.
    fn some_instance() -> InstanceId {
        InstanceId::from(AgentId::random())
    }

    /// A notifier over a throwaway in-memory database, built directly rather
    /// than through [`install`] so tests don't fight over the process static.
    ///
    /// The realm comes back too: `ResidentVec` updates its own view from a
    /// watcher task, so assertions read the database rather than racing it.
    fn notifier() -> Result<(Notifier, crate::database::RealmDatabase)> {
        let database = test_db!(NotificationData);
        let realm = database.realm(RealmName::default())?;
        Ok((
            Notifier {
                data: realm.resident_vec(())?,
                instance_id: some_instance(),
            },
            realm,
        ))
    }

    /// The titles actually stored, sorted.
    fn stored_titles(realm: &crate::database::RealmDatabase) -> Result<Vec<String>> {
        let r = realm.r_transaction()?;
        let mut titles: Vec<String> = r
            .scan()
            .primary::<NotificationData>()?
            .all()?
            .collect::<std::result::Result<Vec<_>, _>>()?
            .into_iter()
            .map(|n| n.title)
            .collect();
        titles.sort();
        Ok(titles)
    }

    /// A notification created `days` ago, as if raised by `instance`.
    fn aged(instance: InstanceId, title: &str, days: i64) -> NotificationData {
        NotificationData {
            layer: "Health".into(),
            severity: Severity::Info,
            title: title.into(),
            body: None,
            subject: None,
            _instance_id: instance,
            _creation: DataCreation::at(Utc::now() - TimeDelta::days(days)),
            _id: Default::default(),
            _revision: Default::default(),
        }
    }

    #[test_log::test(tokio::test)]
    async fn raise_stores_the_notification() -> Result<()> {
        let (notifier, _realm) = notifier()?;
        notifier.raise(
            Notification::error("Health", "sshd.service failed")
                .body("The unit entered the failed state"),
        )?;

        assert_eq!(notifier.data.len(), 1);
        let stored = notifier.data.iter().next().unwrap();
        let stored = stored.read();
        assert_eq!(stored.layer, "Health");
        assert_eq!(stored.severity, Severity::Error);
        assert_eq!(stored.title, "sshd.service failed");
        assert_eq!(
            stored.body.as_deref(),
            Some("The unit entered the failed state")
        );
        // The raising instance owns the row, which is what makes it replicate.
        assert_eq!(stored._instance_id, notifier.instance_id);
        Ok(())
    }

    #[test_log::test(tokio::test)]
    async fn severity_helpers_pick_the_right_level() {
        assert_eq!(Notification::info("L", "t").severity, Severity::Info);
        assert_eq!(Notification::warn("L", "t").severity, Severity::Warn);
        assert_eq!(Notification::error("L", "t").severity, Severity::Error);
    }

    #[test_log::test(tokio::test)]
    async fn trim_drops_our_expired_rows_and_keeps_the_rest() -> Result<()> {
        let (notifier, realm) = notifier()?;
        let us = notifier.instance_id;

        notifier.data.push(aged(us, "ancient", RETENTION_DAYS + 1))?;
        notifier.data.push(aged(us, "fresh", 1))?;

        notifier.trim();

        assert_eq!(stored_titles(&realm)?, vec!["fresh".to_string()]);
        Ok(())
    }

    #[test_log::test(tokio::test)]
    async fn trim_keeps_another_instances_expired_rows() -> Result<()> {
        let (notifier, realm) = notifier()?;

        // Replicated from elsewhere: expired, but not ours to delete — its
        // owner trims it and the removal replicates here.
        notifier
            .data
            .push(aged(some_instance(), "someone else's", RETENTION_DAYS + 1))?;

        notifier.trim();
        assert_eq!(stored_titles(&realm)?, vec!["someone else's".to_string()]);
        Ok(())
    }
}
