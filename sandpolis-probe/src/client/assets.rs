//! Compile-time-embedded probe-layer assets (the `probe/*.svg` node icons).
//!
//! These are overlaid onto the client's embedded asset bundle by the GUI
//! bootstrap so probe node SVGs resolve through bevy's `AssetServer` in a
//! self-contained release build.

sandpolis_client::embedded_assets!();
