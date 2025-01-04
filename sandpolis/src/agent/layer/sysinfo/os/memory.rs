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
