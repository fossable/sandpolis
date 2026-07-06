//! Cross-platform screen capture.
//!
//! Adapted from rustdesk's `scrap` crate (MIT), itself a fork of
//! quadrupleslap/scrap. The codec, recording and camera machinery was
//! dropped; only raw frame capture remains.

pub use common::*;

#[cfg(quartz)]
pub mod quartz;

#[cfg(x11)]
pub mod x11;

#[cfg(x11)]
pub mod wayland;

#[cfg(dxgi)]
pub mod dxgi;

#[cfg(target_os = "android")]
pub mod android;

#[cfg(x11)]
pub(crate) mod config;

mod common;
