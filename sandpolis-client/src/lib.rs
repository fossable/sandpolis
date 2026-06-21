#[cfg(not(target_os = "android"))]
pub mod cli;
pub mod config;

#[cfg(feature = "client")]
pub mod sync;

#[cfg(feature = "client")]
pub mod gui;

#[cfg(all(feature = "client", not(target_os = "android")))]
pub mod tui;
