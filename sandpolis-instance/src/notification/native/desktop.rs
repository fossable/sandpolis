//! Desktop notifications on Linux, Windows, and macOS.
//!
//! `notify-rust` covers all three behind one builder — the freedesktop D-Bus
//! interface on Linux (over `zbus`, the same stack the rest of the workspace
//! uses), WinRT toasts on Windows, and the user-notification API on macOS.
//! `appname` is a silent no-op off Linux.

use super::warn_once;
use crate::notification::Severity;

pub fn show(title: &str, body: Option<&str>, severity: Severity) {
    let mut notification = notify_rust::Notification::new();
    notification
        .appname("Sandpolis")
        .summary(title)
        .urgency(match severity {
            Severity::Info => notify_rust::Urgency::Low,
            Severity::Warn => notify_rust::Urgency::Normal,
            Severity::Error => notify_rust::Urgency::Critical,
        });

    if let Some(body) = body {
        notification.body(body);
    }

    // A desktop session with no notification daemon (a bare X server, a
    // headless run) fails here every time, which is what `warn_once` is for.
    if let Err(e) = notification.show() {
        warn_once(format!("Desktop notifications are unavailable: {e}"));
    }
}
