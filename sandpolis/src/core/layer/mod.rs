#[cfg(feature = "layer-desktop")]
pub mod desktop;
#[cfg(feature = "layer-filesystem")]
pub mod filesystem;
#[cfg(feature = "layer-location")]
pub mod location;
#[cfg(feature = "layer-package")]
pub mod package;
#[cfg(feature = "layer-probe")]
pub mod probe;
#[cfg(feature = "layer-shell")]
pub mod shell;
#[cfg(feature = "layer-sysinfo")]
pub mod sysinfo;
#[cfg(feature = "layer-tunnel")]
pub mod tunnel;

// These layers are required
pub mod agent;
pub mod network;
pub mod server;
