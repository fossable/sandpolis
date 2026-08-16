use serde::{Deserialize, Serialize};

#[cfg(feature = "client")]
pub mod client;
pub mod streams;

#[derive(Clone, Serialize, Deserialize)]
pub enum WakeAction {
    Poweroff,
    Reboot,
}
