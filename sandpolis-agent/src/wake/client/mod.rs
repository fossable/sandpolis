#[cfg(all(feature = "client", not(target_os = "android")))]
pub mod tui;
