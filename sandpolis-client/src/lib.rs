#[cfg(not(target_os = "android"))]
pub mod cli;

#[cfg(feature = "client")]
pub mod service;

#[cfg(feature = "client")]
pub mod sync;

#[cfg(feature = "client")]
pub mod gui;

#[cfg(all(feature = "client", not(target_os = "android")))]
pub mod tui;
