use crate::agent::Monitor;
use crate::core::database::Document;
use crate::core::layer::sysinfo::os::memory::MemoryData;
use anyhow::Result;
use sysinfo::{MemoryRefreshKind, RefreshKind, System};
use tracing::trace;

pub struct MemoryMonitor {
    document: Document<MemoryData>,
    system: System,
}

impl MemoryMonitor {
    pub fn new(document: Document<MemoryData>) -> Self {
        Self {
            document,
            system: System::new_with_specifics(
                RefreshKind::nothing().with_memory(MemoryRefreshKind::everything()),
            ),
        }
    }
}

impl Monitor for MemoryMonitor {
    fn refresh(&mut self) -> Result<()> {
        self.system.refresh_memory();
        self.document.mutate(|data| {
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
    use crate::{agent::Monitor, core::database::Database};
    use anyhow::Result;

    #[test]
    fn test_memory_monitor() -> Result<()> {
        let db_path = tempfile::tempdir()?;
        let db = Database::new(db_path)?;

        let mut monitor = super::MemoryMonitor::new(db.document("/test")?);
        monitor.refresh()?;

        assert!(monitor.document.data.total > 0);
        Ok(())
    }
}
