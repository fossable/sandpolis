use native_db::ToKey;
use native_model::Model;
use sandpolis_instance::InstanceId;
use sandpolis_macros::data;
use serde::{Deserialize, Serialize};
use strum::{Display, EnumString};

#[cfg(feature = "agent")]
pub mod agent;

/// The high-level activation state of a systemd unit.
#[derive(Serialize, Deserialize, Clone, Debug, Default, PartialEq, Eq, Display, EnumString)]
#[strum(serialize_all = "kebab-case")]
pub enum ActiveState {
    Active,
    Reloading,
    #[default]
    Inactive,
    Failed,
    Activating,
    Deactivating,
    #[strum(default)]
    Unknown(String),
}

/// Information about a single systemd unit on an agent.
#[data]
#[derive(Default)]
pub struct SystemdUnitData {
    #[secondary_key]
    pub _instance_id: InstanceId,

    /// The unit's primary name, e.g. "sshd.service"
    #[secondary_key]
    pub name: String,
    /// Human-readable description of the unit
    pub description: Option<String>,
    /// Reflects whether the unit definition was properly loaded
    /// (e.g. "loaded", "not-found", "masked")
    pub load_state: Option<String>,
    /// High-level activation state
    pub active_state: ActiveState,
    /// Low-level unit-type-specific state (e.g. "running", "dead", "exited")
    pub sub_state: Option<String>,
}

inventory::submit! {
    sandpolis_instance::database::sync::SyncRegistration(|r| {
        r.register_scoped::<SystemdUnitData>(|d| d._instance_id)
    })
}
