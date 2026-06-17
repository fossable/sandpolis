use crate::systemd::{ActiveState, SystemdUnitData};
use native_model::Model;
use sandpolis_instance::InstanceId;
use sandpolis_instance::realm::RealmName;

#[cfg(feature = "client-gui")]
pub mod gui;
#[cfg(feature = "client-tui")]
pub mod tui;

/// A flattened view of a systemd unit for client display.
#[derive(Clone, Debug)]
pub struct SystemdUnitInfo {
    pub name: String,
    pub description: Option<String>,
    pub active_state: ActiveState,
    pub sub_state: Option<String>,
}

impl From<SystemdUnitData> for SystemdUnitInfo {
    fn from(value: SystemdUnitData) -> Self {
        Self {
            name: value.name,
            description: value.description,
            active_state: value.active_state,
            sub_state: value.sub_state,
        }
    }
}

/// The sync model id for systemd units (used to subscribe to live updates).
pub fn systemd_model_id() -> u32 {
    <SystemdUnitData as Model>::native_model_id()
}

/// Subscribe to live systemd updates for an instance (call when a view opens).
pub fn subscribe(instance: InstanceId) {
    sandpolis_client::sync::subscribe(systemd_model_id(), Some(instance));
}

/// Unsubscribe from systemd updates for an instance (call when a view closes).
pub fn unsubscribe(instance: InstanceId) {
    sandpolis_client::sync::unsubscribe(systemd_model_id(), Some(instance));
}

/// Query the systemd units known for an instance from the client's local
/// database (populated by the sync subscription).
pub fn query_systemd_units(id: InstanceId) -> anyhow::Result<Vec<SystemdUnitInfo>> {
    let Some(database) = sandpolis_client::sync::client_database() else {
        return Ok(vec![]);
    };
    let realm = database.realm(RealmName::default())?;
    let r = realm.r_transaction()?;
    let units: Vec<SystemdUnitData> = r
        .scan()
        .primary::<SystemdUnitData>()?
        .all()?
        .collect::<std::result::Result<Vec<_>, _>>()?;
    Ok(units
        .into_iter()
        .filter(|u| u._instance_id == id)
        .map(Into::into)
        .collect())
}
