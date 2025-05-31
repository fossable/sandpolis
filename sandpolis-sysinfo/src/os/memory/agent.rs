use super::MemoryData;
use anyhow::Result;
use native_db::Database;
use sandpolis_agent::Collector;
use sandpolis_database::Document;
use std::sync::Arc;
use sysinfo::{MemoryRefreshKind, RefreshKind, System};
use tracing::trace;

pub struct MemoryMonitor {
    db: Arc<Database<'static>>,
    system: System,
    data: Option<MemoryData>,
}

impl MemoryMonitor {
    pub fn new(db: Arc<Database<'static>>) -> Self {
        Self {
            db,
            system: System::new_with_specifics(
                RefreshKind::nothing().with_memory(MemoryRefreshKind::everything()),
            ),
            data: None,
        }
    }
}

impl Collector for MemoryMonitor {
    fn refresh(&mut self) -> Result<()> {
        self.system.refresh_memory();

        let next: MemoryData = (&self.system).into();
        trace!(next = ?next, "Polled memory info");

        if let Some(previous) = self.data.as_ref() {
            if *previous != next {
                self.data = Some(next.clone());

                let rw = self.db.rw_transaction()?;
                rw.insert(next)?;
                rw.commit()?;
            }
        }

        Ok(())
    }
}

impl From<&System> for MemoryData {
    fn from(value: &System) -> Self {
        Self {
            total: value.total_memory(),
            free: value.free_memory(),
            swap_total: value.total_swap(),
            swap_free: value.free_swap(),
            ..Default::default()
        }
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
