//! End-to-end cover for the client's notification surfacing, minus the UI.
//!
//! Everything here hangs off process-global state — the notifier, the
//! watermark, the toast sink are all `OnceLock`s — so this is deliberately one
//! test rather than several. Splitting it would let the cases race over the
//! same statics.

#![cfg(feature = "client")]

use anyhow::Result;
use chrono::{TimeDelta, Utc};
use native_db::Models;
use sandpolis_client::notification::NotificationWatermarkData;
use sandpolis_instance::AgentId;
use sandpolis_instance::database::{
    DataCreation, DatabaseManager, WriteAuthority, config::DatabaseConfig,
};
use sandpolis_instance::notification::{Notification, NotificationData, install, notify};
use sandpolis_instance::realm::RealmName;
use std::sync::LazyLock;
use std::time::Duration;

static MODELS: LazyLock<Models> = LazyLock::new(|| {
    let mut models = Models::new();
    models.define::<NotificationData>().unwrap();
    models.define::<NotificationWatermarkData>().unwrap();
    models
});

#[tokio::test]
async fn surfaces_new_notifications_but_not_replayed_history() -> Result<()> {
    let database = DatabaseManager::new(
        DatabaseConfig {
            storage: None,
            key: Default::default(),
        },
        &MODELS,
        WriteAuthority::Full,
    )?;
    let realm = database.realm(RealmName::default())?;
    let instance_id = AgentId::random().into();

    install(&realm, instance_id);

    let (sender, mut toasts) = tokio::sync::mpsc::unbounded_channel();
    sandpolis_client::notification::set_toast_sink(sender);
    // Pretend the window has focus, so delivery goes to the channel instead of
    // the developer's actual desktop.
    sandpolis_client::notification::set_foreground(true);

    sandpolis_client::notification::watch(&database)?;

    // A notification raised now is new, so it should arrive.
    notify(Notification::warn("Health", "sshd.service failed").body("Unit entered failed state"));

    let surfaced = tokio::time::timeout(Duration::from_secs(5), toasts.recv())
        .await
        .expect("a notification should have been surfaced")
        .expect("the toast channel should still be open");
    assert_eq!(surfaced.title, "sshd.service failed");
    assert_eq!(surfaced.layer, "Health");

    // A row predating the watermark stands in for a subscription's opening
    // snapshot: already-seen history, which must not be announced again.
    let notifier = sandpolis_instance::notification::handle().expect("the notifier is installed");
    notifier.data().push(NotificationData {
        layer: "Health".into(),
        severity: Default::default(),
        title: "old news".into(),
        body: None,
        subject: None,
        _instance_id: instance_id,
        _creation: DataCreation::at(Utc::now() - TimeDelta::days(1)),
        _id: Default::default(),
        _revision: Default::default(),
    })?;

    match tokio::time::timeout(Duration::from_millis(500), toasts.recv()).await {
        Err(_) => {}
        Ok(other) => panic!("history below the watermark was surfaced: {other:?}"),
    }

    Ok(())
}
