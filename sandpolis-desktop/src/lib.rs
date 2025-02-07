use axum::Router;

pub(crate) mod messages;

#[cfg(feature = "agent")]
pub mod agent;

#[cfg(feature = "client")]
pub mod client;

#[derive(Clone)]
pub struct DesktopLayer {}

// TODO agent lists available desktops for db
