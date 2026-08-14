//! Notification example, exercising the framework end to end without needing
//! anything to actually go wrong:
//!
//! - one notification per severity, raised through the same `notify` call any
//!   layer would use
//! - toast stacking, the visible cap, and the fade-out
//! - the focus split: toasts while the window has focus, native OS
//!   notifications while it doesn't (click away from the window to see it)
//!
//! ```sh
//! cargo run --example client_gui_notification --features client
//! ```
use anyhow::Result;
use sandpolis::{InstanceState, MODELS, RuntimeOptions};
use sandpolis_instance::database::{DatabaseLayer, WriteAuthority, config::DatabaseConfig};
use sandpolis_instance::notification::{Notification, notify};
use sandpolis_instance::realm::Realms;
use sandpolis_server::ServerStratum;
use std::time::Duration;

/// Gap between raised notifications. Longer than it takes to alt-tab, so the
/// focused and unfocused paths are both easy to catch.
const INTERVAL: Duration = Duration::from_secs(4);

#[tokio::main]
async fn main() -> Result<()> {
    let options = RuntimeOptions::embedded();

    let database = DatabaseLayer::new(
        DatabaseConfig {
            storage: None,
            key: Default::default(),
        },
        &*MODELS,
        WriteAuthority::Full,
    )?;

    let realms = Realms::for_client(Vec::new(), database.clone())?;
    let state = InstanceState::new(&options, database, realms, ServerStratum::Global).await?;

    // The client watches the database from `spawn_client_sync`, which the GUI
    // calls on startup. Give it a moment, then raise notifications on a loop —
    // anything raised before the watcher exists is below the watermark and is
    // deliberately not surfaced.
    tokio::spawn(async move {
        tokio::time::sleep(Duration::from_secs(2)).await;

        let mut round = 1;
        loop {
            notify(
                Notification::info("Health", format!("Routine check {round}"))
                    .body("Everything is where it should be"),
            );
            tokio::time::sleep(INTERVAL).await;

            notify(
                Notification::warn("Probe", "camera-03 is slow to respond")
                    .body("Three consecutive probes took over 5s"),
            );
            tokio::time::sleep(INTERVAL).await;

            notify(
                Notification::error("Health", "sshd.service failed")
                    .body("The unit entered the failed state"),
            );
            tokio::time::sleep(INTERVAL).await;

            // A burst, to show the stack cap shedding the oldest toasts.
            for n in 1..=8 {
                notify(Notification::info("Audit", format!("Burst event {n}")));
            }
            tokio::time::sleep(INTERVAL).await;

            round += 1;
        }
    });

    sandpolis::client::gui::main(options, state).await
}
