//! Compile-time-embedded probe-layer assets (the `probe/*.svg` node icons).
//!
//! These are overlaid onto the client's embedded asset bundle by the GUI
//! bootstrap so probe node SVGs resolve through bevy's [`AssetServer`] in a
//! self-contained release build.

use include_dir::{Dir, include_dir};

/// Compile-time snapshot of `sandpolis-probe/assets`.
static ASSETS: Dir<'static> = include_dir!("$CARGO_MANIFEST_DIR/assets");

/// The embedded probe asset directory, for the GUI bootstrap to merge into the
/// default asset source.
pub fn dir() -> &'static Dir<'static> {
    &ASSETS
}
