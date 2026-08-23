//! Watches the data directory so a manual edit to a realm config is noticed
//! while the server runs.
//!
//! The server can't apply such an edit — realms are frozen at startup — but it
//! can warn that a restart is needed and stop writing the file back, so the
//! edit isn't clobbered by the next persist. Enforcement itself lives in
//! [`RealmConfig`]'s write path, which compares on-disk bytes under the file
//! lock; this watcher exists to surface the warning promptly rather than on
//! the next write attempt.
//!
//! [`RealmConfig`]: crate::config::RealmConfig

use crate::config;
use anyhow::Result;
use fs2::FileExt;
use notify::Watcher;
use sandpolis_instance::realm::RealmName;
use std::collections::HashSet;
use std::io::Read;
use std::path::{Path, PathBuf};
use std::sync::Arc;
use std::time::Duration;
use tracing::{debug, warn};

/// Editors save with a burst of writes and renames, so wait for the burst to
/// settle before sweeping.
const DEBOUNCE: Duration = Duration::from_millis(500);

/// Watch `data_dir` for changes to realm configs until the server exits.
pub async fn watch_realm_configs(data_dir: PathBuf) -> Result<()> {
    let wake = Arc::new(tokio::sync::Notify::new());
    let mut watcher = notify::recommended_watcher({
        let wake = wake.clone();
        // Any event — or a watch error — just triggers a sweep, which
        // sidesteps classifying the write/rename patterns editors produce.
        move |_result: notify::Result<notify::Event>| {
            wake.notify_one();
        }
    })?;
    watcher.watch(&data_dir, notify::RecursiveMode::NonRecursive)?;

    let mut warned_foreign = HashSet::new();
    loop {
        // The first pass covers edits made before the watch was registered;
        // `Notify` holds a permit, so events during a sweep aren't lost.
        sweep(&data_dir, &mut warned_foreign);
        wake.notified().await;
        tokio::time::sleep(DEBOUNCE).await;
    }
}

/// Compare every realm config in the directory against what the server last
/// synced. A full sweep per wake-up is cheap — a data directory holds a
/// handful of realms — and catches new and deleted files for free.
fn sweep(data_dir: &Path, warned_foreign: &mut HashSet<String>) {
    let entries = match std::fs::read_dir(data_dir) {
        Ok(entries) => entries,
        Err(e) => {
            warn!(path = %data_dir.display(), error = %e, "Failed to sweep the data directory");
            return;
        }
    };

    let mut seen = HashSet::new();
    for entry in entries.flatten() {
        let path = entry.path();
        let Ok(stem) = config::realm_name(&path) else {
            continue;
        };

        let Ok(name) = stem.parse::<RealmName>() else {
            if warned_foreign.insert(stem.to_string()) {
                warn!(
                    path = %path.display(),
                    "A file named like a realm config appeared, but its realm name is invalid"
                );
            }
            continue;
        };
        seen.insert(name.clone());

        match config::synced_contents(&name) {
            None => {
                if warned_foreign.insert(name.to_string()) {
                    warn!(
                        realm = %name,
                        path = %path.display(),
                        "A new realm config appeared; restart the server to serve it"
                    );
                }
            }
            Some(None) => {
                debug!(realm = %name, "Realm config is already read-only");
            }
            Some(Some(_)) => {
                if let Err(e) = check_file(&path, &name) {
                    warn!(
                        realm = %name,
                        path = %path.display(),
                        error = %e,
                        "Failed to check the realm config for manual edits"
                    );
                }
            }
        }
    }

    for name in config::synced_realms_in(data_dir) {
        if !seen.contains(&name) && config::mark_read_only(&name) {
            warn!(
                realm = %name,
                "Realm config was deleted; the server will not recreate it — \
                 restart to apply the change"
            );
        }
    }
}

/// Compare one config's on-disk bytes against the registry, holding a shared
/// lock so the read can't interleave with a server write: writers update the
/// registry before releasing their exclusive lock, so file and registry agree
/// whenever this lock is held.
fn check_file(path: &Path, name: &RealmName) -> Result<()> {
    let mut file = std::fs::File::open(path)?;
    FileExt::lock_shared(&file)?;

    let mut contents = String::new();
    file.read_to_string(&mut contents)?;

    let Some(Some(expected)) = config::synced_contents(name) else {
        return Ok(());
    };
    if contents != expected && config::mark_read_only(name) {
        warn!(
            realm = %name,
            path = %path.display(),
            "Realm config was edited on disk; the server will no longer write \
             it — restart to apply the changes"
        );
    }
    Ok(())
}

#[cfg(test)]
mod test_config_watch {
    use super::*;
    use crate::config::RealmConfig;

    // Realm names in these tests are unique across the whole test binary: the
    // sync registry is process-global and tests run in parallel.

    /// A manual edit found by a sweep makes the realm read-only.
    #[test]
    fn sweep_detects_manual_edit() -> Result<()> {
        let dir = tempfile::tempdir()?;
        let path = dir.path().join("sweepedit.realm.ron");
        std::fs::write(&path, "")?;
        let _config = RealmConfig::load(&path)?;

        std::fs::write(&path, r#"(address: "gs.example.com:9000")"#)?;
        sweep(dir.path(), &mut HashSet::new());

        assert!(config::is_read_only(&"sweepedit".parse()?));
        Ok(())
    }

    /// An untouched config stays writable across sweeps.
    #[test]
    fn sweep_leaves_unchanged_configs_alone() -> Result<()> {
        let dir = tempfile::tempdir()?;
        let path = dir.path().join("sweepsame.realm.ron");
        std::fs::write(&path, "")?;
        let _config = RealmConfig::load(&path)?;

        sweep(dir.path(), &mut HashSet::new());
        sweep(dir.path(), &mut HashSet::new());

        assert!(!config::is_read_only(&"sweepsame".parse()?));
        Ok(())
    }

    /// A config the server never loaded is flagged once, not on every sweep.
    #[test]
    fn sweep_warns_about_new_configs_once() -> Result<()> {
        let dir = tempfile::tempdir()?;
        std::fs::write(dir.path().join("sweepnew.realm.ron"), "")?;

        let mut warned = HashSet::new();
        sweep(dir.path(), &mut warned);
        assert!(warned.contains("sweepnew"));

        sweep(dir.path(), &mut warned);
        assert_eq!(warned.len(), 1);
        Ok(())
    }

    /// A deleted config becomes read-only, so the next persist doesn't quietly
    /// recreate it.
    #[test]
    fn sweep_detects_deletion() -> Result<()> {
        let dir = tempfile::tempdir()?;
        let path = dir.path().join("sweepgone.realm.ron");
        std::fs::write(&path, "")?;
        let _config = RealmConfig::load(&path)?;

        std::fs::remove_file(&path)?;
        sweep(dir.path(), &mut HashSet::new());

        assert!(config::is_read_only(&"sweepgone".parse()?));
        Ok(())
    }
}
