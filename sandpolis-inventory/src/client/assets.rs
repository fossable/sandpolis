//! Compile-time-embedded inventory-layer assets (the `inventory/*.svg` device
//! class icons).
//!
//! These are overlaid onto the client's embedded asset bundle by the GUI
//! bootstrap so inventory node SVGs resolve through bevy's `AssetServer` in a
//! self-contained release build.

sandpolis_client::embedded_assets!();
