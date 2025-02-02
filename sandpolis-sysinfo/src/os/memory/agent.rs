use super::MemoryData;
use anyhow::Result;
use sandpolis_agent::Collector;
use sandpolis_database::Document;
use sysinfo::{MemoryRefreshKind, RefreshKind, System};
use tracing::trace;

pub struct MemoryMonitor {
    data: Document<MemoryData>,
    system: System,
}

impl MemoryMonitor {
    pub fn new(data: Document<MemoryData>) -> Self {
        Self {
            data,
            system: System::new_with_specifics(
                RefreshKind::nothing().with_memory(MemoryRefreshKind::everything()),
            ),
        }
    }
}

impl Collector for MemoryMonitor {
    fn refresh(&mut self) -> Result<()> {
        self.system.refresh_memory();
        self.data.mutate(|data| {
            data.total = self.system.total_memory();
            data.free = self.system.free_memory();
            data.swap_free = self.system.free_swap();
            data.swap_total = self.system.total_swap();

            trace!(data = ?data, "Polled memory info");
            Ok(())
        });

        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use crate::{agent::layer::agent::Collector, core::database::Database};
    use anyhow::Result;

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
