use super::MemoryData;
use anyhow::Result;
use sandpolis_agent::Collector;
use sandpolis_instance::InstanceId;
use sandpolis_instance::database::{RealmDatabase, Resident};
use sysinfo::{MemoryRefreshKind, RefreshKind, System};
use tracing::trace;

pub struct MemoryMonitor {
    system: System,
    data: Resident<MemoryData>,
    instance_id: InstanceId,
}

impl MemoryMonitor {
    pub fn new(db: RealmDatabase, instance_id: InstanceId) -> Result<Self> {
        Ok(Self {
            system: System::new_with_specifics(
                RefreshKind::nothing().with_memory(MemoryRefreshKind::everything()),
            ),
            data: db.resident(())?,
            instance_id,
        })
    }
}

impl Collector for MemoryMonitor {
    async fn refresh(&mut self) -> Result<()> {
        self.system.refresh_memory();
        trace!(info = ?self.system, "Polled memory info");

        self.data.update(|data| {
            data._instance_id = self.instance_id;
            data.total = self.system.total_memory();
            data.free = self.system.free_memory();
            data.swap_total = self.system.total_swap();
            data.swap_free = self.system.free_swap();
            Ok(())
        })?;

        Ok(())
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
    async fn test_memory_monitor() -> Result<()> {
        let database: DatabaseLayer = test_db!(MemoryData);

        let instance_id = InstanceId::new_server();
        let mut monitor =
            MemoryMonitor::new(database.realm(RealmName::default())?, instance_id)?;
        monitor.refresh().await?;

        assert!(monitor.data.read().total > 0);
        assert_eq!(monitor.data.read()._instance_id, instance_id);
        Ok(())
    }
}
