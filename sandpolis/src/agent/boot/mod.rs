//! Fullscreen UI for boot mode (the `uki` feature), where the agent is a chainloader
//! that runs before the actual OS: a homepage counts down toward booting the
//! first UEFI entry unless a key is pressed or the server places a boot hold,
//! and a held agent swaps to the snapshot block-grid display while the server
//! runs snapshot operations against the cold partitions.
//!
//! The slint event loop owns the main thread (winit on a desktop for
//! development; in the UKI the linuxkms backend draws straight to the display,
//! selected with `SLINT_BACKEND=linuxkms-noseat`). The regular agent runs
//! alongside on the tokio workers and communicates through the shared
//! [`BootAgentState`].

use anyhow::Result;
use sandpolis_agent::bootagent::BootAgentState;
use sandpolis_agent::uefi::{self, BootEntry};
use slint::{ComponentHandle, ModelRc, SharedString, Timer, TimerMode, VecModel};
use std::rc::Rc;
use std::sync::Arc;
use std::sync::atomic::Ordering;
use std::time::{Duration, Instant};
use tracing::{info, warn};

slint::include_modules!();

/// How long the countdown runs before the first entry boots automatically.
const COUNTDOWN: Duration = Duration::from_secs(10);

/// How often the UI samples the shared state.
const SAMPLE_INTERVAL: Duration = Duration::from_millis(100);

/// How long a finished snapshot grid stays visible before the homepage
/// returns.
#[cfg(feature = "snapshot")]
const SNAPSHOT_LINGER: Duration = Duration::from_secs(3);

/// The chainloadable boot entries: everything in `BootOrder` except the boot
/// agent's own entry. Enumeration errors (no UEFI, no efivars access) yield an
/// empty menu so the UI still comes up on development machines.
pub fn chainload_entries() -> Vec<BootEntry> {
    let current = uefi::current_boot().ok();
    match uefi::boot_entries() {
        Ok(entries) => entries
            .into_iter()
            .filter(|entry| Some(entry.number) != current)
            .collect(),
        Err(e) => {
            warn!(error = %e, "Failed to enumerate UEFI boot entries");
            Vec::new()
        }
    }
}

/// Chainload a boot entry by pointing `BootNext` at it and rebooting, or just
/// reboot for `None` (a released hold boots the restored OS normally).
///
/// The reboot is spawned onto the runtime because this is called from slint
/// callbacks on the UI thread.
pub fn chainload(handle: &tokio::runtime::Handle, entry: Option<&BootEntry>) {
    if let Some(entry) = entry {
        info!(number = entry.number, description = %entry.description, "Chainloading boot entry");
        if let Err(e) = uefi::set_boot_next(entry.number) {
            warn!(error = %e, "Failed to set BootNext; rebooting anyway");
        }
    } else {
        info!("Rebooting");
    }
    handle.spawn(async {
        use sandpolis_agent::wake::{WakeAction, change_power_state};
        if let Err(e) = change_power_state(&WakeAction::Reboot).await {
            warn!(error = %e, "Failed to reboot");
        }
    });
}

/// The source address this device would use to reach the server (or, before a
/// server is known, the internet): a UDP "connect" picks it without sending
/// anything.
fn local_ip(server: Option<&str>) -> Option<std::net::IpAddr> {
    let socket = std::net::UdpSocket::bind("0.0.0.0:0").ok()?;
    if !server.is_some_and(|server| socket.connect(server).is_ok()) {
        socket.connect("1.1.1.1:443").ok()?;
    }
    Some(socket.local_addr().ok()?.ip())
}

/// Run the homepage on the calling thread until the process ends (a boot agent
/// only exits by rebooting). `boot` performs the actual chainload — `None`
/// means plain reboot — so the example can substitute a mock.
pub fn run_boot_ui(
    state: Arc<BootAgentState>,
    entries: Vec<BootEntry>,
    boot: impl Fn(Option<&BootEntry>) + 'static,
) -> Result<()> {
    let ui = BootHomepage::new()?;
    let boot = Rc::new(boot);
    let entries = Rc::new(entries);

    ui.set_entries(ModelRc::new(VecModel::from(
        entries
            .iter()
            .map(|entry| SharedString::from(entry.description.as_str()))
            .collect::<Vec<_>>(),
    )));

    // Key handling: the first press cancels the countdown; arrows and enter
    // drive the menu afterwards.
    {
        let ui_weak = ui.as_weak();
        let entries = entries.clone();
        let boot = boot.clone();
        ui.on_key(move |key| {
            let Some(ui) = ui_weak.upgrade() else { return };
            match ui.get_mode() {
                BootMode::Countdown => ui.set_mode(BootMode::Menu),
                BootMode::Menu => match key.as_str() {
                    "up" => ui.set_selected((ui.get_selected() - 1).max(0)),
                    "down" => {
                        ui.set_selected((ui.get_selected() + 1).min(entries.len() as i32 - 1))
                    }
                    "enter" => {
                        if let Some(entry) = entries.get(ui.get_selected().max(0) as usize) {
                            boot(Some(entry));
                        }
                    }
                    _ => {}
                },
                BootMode::Hold => {}
            }
        });
    }

    // Sample loop: drains the countdown, mirrors the shared state into the
    // UI, and swaps to the snapshot grid during a hold.
    let started = Instant::now();
    let timer = Timer::default();
    {
        let ui_weak = ui.as_weak();
        let mut booted = false;
        let mut tick = 0u32;

        #[cfg(feature = "snapshot")]
        let active = sandpolis_snapshot::boot_snapshot::active();
        #[cfg(feature = "snapshot")]
        let mut grid: Option<SnapshotGrid> = None;

        timer.start(TimerMode::Repeated, SAMPLE_INTERVAL, move || {
            let Some(ui) = ui_weak.upgrade() else { return };

            // Mirror the connection state; the IP lookup is a syscall, so
            // only every couple of seconds
            if tick % 20 == 0 {
                let server = state.server.lock().unwrap().clone();
                ui.set_ip(
                    local_ip(server.as_deref())
                        .map(|ip| ip.to_string())
                        .unwrap_or_default()
                        .into(),
                );
                ui.set_server(server.unwrap_or_default().into());
            }
            tick += 1;

            // A hold cancels the countdown and parks the UI
            if state.hold.load(Ordering::Relaxed) && ui.get_mode() != BootMode::Hold {
                info!("Boot hold detected");
                ui.set_mode(BootMode::Hold);
                ui.set_message(
                    "Boot hold detected — this device will reboot automatically \
                     when the server releases it."
                        .into(),
                );
            }

            // The release instruction boots the (possibly restored) OS
            if state.release.load(Ordering::Relaxed) && !booted {
                booted = true;
                boot(None);
            }

            if ui.get_mode() == BootMode::Countdown {
                let remaining = COUNTDOWN.saturating_sub(started.elapsed());
                ui.set_countdown(remaining.as_secs_f32() / COUNTDOWN.as_secs_f32());
                match entries.first() {
                    Some(first) => {
                        ui.set_countdown_label(
                            format!(
                                "Booting {} in {}s — press any key to cancel",
                                first.description,
                                remaining.as_secs_f32().ceil() as u64,
                            )
                            .into(),
                        );
                        if remaining.is_zero() && !booted {
                            booted = true;
                            boot(Some(first));
                        }
                    }
                    // Nothing to boot into; fall through to the manual menu
                    None => ui.set_mode(BootMode::Menu),
                }
            }

            #[cfg(feature = "snapshot")]
            update_snapshot_grid(&ui, &active, &mut grid);
        });
    }

    // Not `ui.run()`: that returns as soon as its own window hides, which is
    // exactly what the snapshot handoff does. A boot agent only exits by
    // rebooting.
    ui.show()?;
    slint::run_event_loop_until_quit()?;
    Ok(())
}

/// The snapshot display riding on the homepage's event loop.
#[cfg(feature = "snapshot")]
struct SnapshotGrid {
    ui: sandpolis_snapshot::boot_snapshot::BootSnapshot,
    _timer: Timer,
    progress: Arc<sandpolis_snapshot::boot_snapshot::SnapshotProgress>,
    finished_at: Option<Instant>,
}

/// Swap between the homepage and the block-grid display as snapshot
/// operations start and finish. The final frame lingers briefly so the
/// outcome is visible before the homepage returns.
#[cfg(feature = "snapshot")]
fn update_snapshot_grid(
    homepage: &BootHomepage,
    active: &tokio::sync::watch::Receiver<
        Option<Arc<sandpolis_snapshot::boot_snapshot::SnapshotProgress>>,
    >,
    grid: &mut Option<SnapshotGrid>,
) {
    use sandpolis_snapshot::boot_snapshot;

    match grid {
        None => {
            let Some(progress) = active.borrow().clone() else {
                return;
            };
            let snapshot_ui = match boot_snapshot::BootSnapshot::new() {
                Ok(ui) => ui,
                Err(e) => {
                    warn!(error = %e, "Failed to create the snapshot display");
                    return;
                }
            };
            let timer = boot_snapshot::attach(&snapshot_ui, progress.clone());
            // Show before hide so one window is always up
            let _ = snapshot_ui.show();
            let _ = homepage.hide();
            *grid = Some(SnapshotGrid {
                ui: snapshot_ui,
                _timer: timer,
                progress,
                finished_at: None,
            });
        }
        Some(shown) => {
            if shown.progress.done.load(Ordering::Relaxed) {
                let finished = shown.finished_at.get_or_insert_with(Instant::now);
                if finished.elapsed() >= SNAPSHOT_LINGER {
                    let _ = homepage.show();
                    let _ = shown.ui.hide();
                    *grid = None;
                }
            }
        }
    }
}
