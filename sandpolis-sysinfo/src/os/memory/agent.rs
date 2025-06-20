use super::MemoryData;
use anyhow::Result;
use native_db::Database;
use sandpolis_agent::Collector;
use sandpolis_database::Resident;
use std::sync::Arc;
use sysinfo::{MemoryRefreshKind, RefreshKind, System};
use tracing::trace;

pub struct MemoryMonitor {
    system: System,
    data: Resident<MemoryData>,
}

impl MemoryMonitor {
    pub fn new(data: Resident<MemoryData>) -> Self {
        Self {
            system: System::new_with_specifics(
                RefreshKind::nothing().with_memory(MemoryRefreshKind::everything()),
            ),
            data,
        }
    }
}

impl Collector for MemoryMonitor {
    fn refresh(&mut self) -> Result<()> {
        self.system.refresh_memory();
        trace!(info = ?system, "Polled memory info");

        self.data.update(|data| {
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
    use anyhow::Result;
    use sandpolis_agent::Collector;
    use sandpolis_database::Database;

    #[test]
    fn test_memory_monitor() -> Result<()> {
        let db_path = tempfile::tempdir()?;
        let db = Database::new(db_path)?;

        let mut monitor = super::MemoryMonitor::new(db.document("/test")?);
        monitor.refresh()?;

        assert!(monitor.data.data.total > 0);
        Ok(())
    }
}
