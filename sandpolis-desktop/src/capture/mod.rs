//! Cross-platform screen capture.
//!
//! Adapted from rustdesk's `scrap` crate (MIT), itself a fork of
//! quadrupleslap/scrap. The codec, recording and camera machinery was
//! dropped; only raw frame capture remains.

pub use common::*;

#[cfg(target_os = "macos")]
pub mod quartz;

#[cfg(target_os = "linux")]
pub mod x11;

#[cfg(target_os = "linux")]
pub mod wayland;

#[cfg(windows)]
pub mod dxgi;

#[cfg(target_os = "android")]
pub mod android;

#[cfg(target_os = "linux")]
pub(crate) mod config;

mod common;
