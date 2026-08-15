//! Writing a layer's runtime state back to the config file.
//!
//! A layer crate can't reference the main crate's `Configuration` — the
//! dependency runs the other way — so the main crate injects a closure that
//! knows how to persist, and the layer calls it whenever its state changes.

use anyhow::Result;
use std::sync::OnceLock;

/// A layer's hook for writing its state back to the realm config.
///
/// Installed once by the top-level `sandpolis` crate (server only). A layer
/// with no hook installed simply doesn't persist, which is what agents and
/// clients want.
pub struct ConfigPersistHook<T: 'static> {
    /// The layer this hook belongs to, used only to name it in the warning
    /// logged when persistence fails.
    layer: &'static str,
    hook: OnceLock<Box<dyn Fn(&[T]) -> Result<()> + Send + Sync>>,
}

impl<T: 'static> ConfigPersistHook<T> {
    /// Declare a layer's hook. Const so it can live in a `static`.
    pub const fn new(layer: &'static str) -> Self {
        Self {
            layer,
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
            tracing::warn!(layer = %self.layer, error = %e, "Failed to persist layer config");
        }
    }
}
