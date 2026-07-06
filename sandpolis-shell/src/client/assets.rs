//! Compile-time-embedded shell-layer assets (the terminal's monospace font).
//!
//! These are overlaid onto the client's embedded asset bundle by the GUI
//! bootstrap so the font resolves through bevy's [`AssetServer`] in a
//! self-contained release build.

use include_dir::{Dir, include_dir};

/// Compile-time snapshot of `sandpolis-shell/assets`.
static ASSETS: Dir<'static> = include_dir!("$CARGO_MANIFEST_DIR/assets");

/// The embedded shell asset directory, for the GUI bootstrap to merge into the
/// default asset source.
pub fn dir() -> &'static Dir<'static> {
    &ASSETS
}
