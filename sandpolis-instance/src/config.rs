//! Writing a subsystem's runtime state back to the config file.
//!
//! A subsystem crate can't reference the main crate's `Configuration` — the
//! dependency runs the other way — so the main crate injects a closure that
//! knows how to persist, and the subsystem calls it whenever its state changes.

use anyhow::Result;
use std::sync::OnceLock;

/// A subsystem's hook for writing its state back to the realm config.
///
/// Installed once by the top-level `sandpolis` crate (server only). A subsystem
/// with no hook installed simply doesn't persist, which is what agents and
/// clients want.
pub struct ConfigPersistHook<T: 'static> {
    /// The subsystem this hook belongs to, used only to name it in the warning
    /// logged when persistence fails.
    subsystem: &'static str,
    hook: OnceLock<Box<dyn Fn(&[T]) -> Result<()> + Send + Sync>>,
}

impl<T: 'static> ConfigPersistHook<T> {
    /// Declare a subsystem's hook. Const so it can live in a `static`.
    pub const fn new(subsystem: &'static str) -> Self {
        Self {
            subsystem,
            hook: OnceLock::new(),
        }
    }

    /// Install the hook. Idempotent: the first caller wins.
    pub fn set(&self, f: impl Fn(&[T]) -> Result<()> + Send + Sync + 'static) {
        let _ = self.hook.set(Box::new(f));
    }

    /// Persist `items` if a hook is installed, otherwise do nothing.
    ///
    /// Failing to write the config file shouldn't take down whatever operation
    /// changed the state, so this warns rather than propagating.
    pub fn persist(&self, items: &[T]) {
        if let Some(f) = self.hook.get()
            && let Err(e) = f(items)
        {
            tracing::warn!(subsystem = %self.subsystem, error = %e, "Failed to persist subsystem config");
        }
    }
}
