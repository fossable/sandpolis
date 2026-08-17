#[cfg(feature = "client")]
pub mod assets;

#[cfg(feature = "client")]
pub mod gui;

#[cfg(all(feature = "client", not(target_os = "android")))]
pub mod tui;
