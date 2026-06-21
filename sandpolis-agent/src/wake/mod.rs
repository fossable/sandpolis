use sandpolis_instance::realm::RealmName;
use serde::{Deserialize, Serialize};

#[cfg(feature = "client")]
pub mod client;
pub mod streams;

#[derive(Clone, Serialize, Deserialize)]
pub enum WakeAction {
    Poweroff,
    Reboot,
}

pub enum WakePermission {
    Poweroff(Vec<RealmName>),
    Reboot(Vec<RealmName>),
}
