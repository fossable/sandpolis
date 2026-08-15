//! Delivery to the operating system's own notification interface.
//!
//! One implementation per platform, selected by `target_os` and re-exported
//! under a single [`deliver`] — the same shape the desktop subsystem uses for its
//! capture and input backends (see `sandpolis-desktop/src/input/mod.rs`).
//!
//! Delivery is best effort and never fails loudly. A platform with no backend,
//! a desktop session with no notification daemon, or an Android build without
//! the app's helper class all end in a single warning per process and no
//! further noise. A notification that can't be shown natively is not worth an
//! error path: the client still has the toast, and the row is still in the
//! database.

use super::{NotificationData, Severity};

#[cfg(any(target_os = "linux", target_os = "windows", target_os = "macos"))]
mod desktop;

#[cfg(target_os = "android")]
mod android;

/// Show `notification` using the platform's native notification interface.
///
/// Returns immediately. The platform calls underneath block — a D-Bus round
/// trip on Linux, attaching to the JVM on Android — and callers are on async
/// runtime threads, so the work happens on a detached thread.
pub fn deliver(notification: &NotificationData) {
    let title = notification.title.clone();
    let body = notification.body.clone();
    let severity = notification.severity;

    if let Err(e) = std::thread::Builder::new()
        .name("notify".into())
        .spawn(move || show(&title, body.as_deref(), severity))
    {
        warn_once(format!("Failed to spawn the notification thread: {e}"));
    }
}

/// Hand one notification to the platform. Blocking.
#[allow(unused_variables)]
fn show(title: &str, body: Option<&str>, severity: Severity) {
    #[cfg(any(target_os = "linux", target_os = "windows", target_os = "macos"))]
    desktop::show(title, body, severity);

    #[cfg(target_os = "android")]
    android::show(title, body, severity);

    #[cfg(not(any(
        target_os = "linux",
        target_os = "windows",
        target_os = "macos",
        target_os = "android"
    )))]
    warn_once("There is no native notification backend for this platform");
}

/// Log `message` the first time delivery fails and stay quiet after that.
///
/// A broken or absent notification daemon fails for every notification, and a
/// warning per notification would be worse than the missing notification.
pub(crate) fn warn_once(message: impl std::fmt::Display) {
    static WARNED: std::sync::Once = std::sync::Once::new();
    WARNED.call_once(|| tracing::warn!("{message}"));
}
