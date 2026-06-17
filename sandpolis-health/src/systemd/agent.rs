use super::{ActiveState, SystemdUnitData};
use anyhow::Result;
use sandpolis_agent::Collector;
use sandpolis_instance::InstanceId;
use sandpolis_instance::database::{RealmDatabase, ResidentVec};
use std::str::FromStr;
use tracing::trace;
use zbus::Connection;

/// A single entry returned by the systemd manager's `ListUnits` method.
///
/// Maps the D-Bus signature `(ssssssouso)`.
#[derive(Debug, serde::Deserialize, zbus::zvariant::Type)]
pub struct UnitListEntry {
    pub name: String,
    pub description: String,
    pub load_state: String,
    pub active_state: String,
    pub sub_state: String,
    pub followed: String,
    pub object_path: zbus::zvariant::OwnedObjectPath,
    pub job_id: u32,
    pub job_type: String,
    pub job_object_path: zbus::zvariant::OwnedObjectPath,
}

#[zbus::proxy(
    interface = "org.freedesktop.systemd1.Manager",
    default_service = "org.freedesktop.systemd1",
    default_path = "/org/freedesktop/systemd1"
)]
pub trait Manager {
    /// Return a list of all currently loaded units.
    fn list_units(&self) -> zbus::Result<Vec<UnitListEntry>>;
}

pub struct SystemdCollector {
    data: ResidentVec<SystemdUnitData>,
    connection: Option<Connection>,
    instance_id: InstanceId,
}

impl SystemdCollector {
    pub fn new(db: RealmDatabase, instance_id: InstanceId) -> Result<Self> {
        Ok(Self {
            data: db.resident_vec(())?,
            connection: None,
            instance_id,
        })
    }

    /// Snapshot the currently known units for uploading to a server.
    pub fn units(&self) -> Vec<SystemdUnitData> {
        self.data.iter().map(|r| r.read().clone()).collect()
    }

    /// Connect to the system bus on first use and reuse the connection after.
    async fn connection(&mut self) -> Result<&Connection> {
        if self.connection.is_none() {
            self.connection = Some(Connection::system().await?);
        }
        Ok(self.connection.as_ref().unwrap())
    }
}

impl Collector for SystemdCollector {
    async fn refresh(&mut self) -> Result<()> {
        let connection = self.connection().await?.clone();
        let manager = ManagerProxy::new(&connection).await?;
        let units = manager.list_units().await?;
        trace!(count = units.len(), "Polled systemd units");

        // Update or add any units reported by systemd
        'next_unit: for unit in &units {
            for resident in self.data.iter() {
                if resident.read().name == unit.name {
                    resident.update(|u| {
                        u.description = Some(unit.description.clone());
                        u.load_state = Some(unit.load_state.clone());
                        u.active_state = ActiveState::from_str(&unit.active_state)
                            .unwrap_or(ActiveState::Unknown(unit.active_state.clone()));
                        u.sub_state = Some(unit.sub_state.clone());
                        Ok(())
                    });
                    continue 'next_unit;
                }
            }
            let mut data: SystemdUnitData = unit.into();
            data._instance_id = self.instance_id;
            self.data.push(data)?;
        }

        Ok(())
    }
}

impl From<&UnitListEntry> for SystemdUnitData {
    fn from(value: &UnitListEntry) -> Self {
        Self {
            name: value.name.clone(),
            description: Some(value.description.clone()),
            load_state: Some(value.load_state.clone()),
            active_state: ActiveState::from_str(&value.active_state)
                .unwrap_or(ActiveState::Unknown(value.active_state.clone())),
            sub_state: Some(value.sub_state.clone()),
            ..Default::default()
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use sandpolis_instance::database::DatabaseLayer;
    use sandpolis_instance::realm::RealmName;
    use sandpolis_instance::test_db;

    #[tokio::test]
    #[test_log::test]
    #[ignore = "requires a running systemd / system D-Bus"]
    async fn test_systemd_collector() -> Result<()> {
        let database: DatabaseLayer = test_db!(SystemdUnitData);

        let mut collector =
            SystemdCollector::new(database.realm(RealmName::default())?, InstanceId::default())?;
        collector.refresh().await?;

        assert!(!collector.data.iter().next().is_none());
        Ok(())
    }
}
