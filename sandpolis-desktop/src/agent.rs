use crate::DesktopData;
use anyhow::Result;
use sandpolis_agent::Collector;
use sandpolis_instance::database::{RealmDatabase, ResidentVec};
use tracing::trace;

/// Enumerates capturable desktops (displays) into the database.
pub struct DesktopDisplayCollector {
    data: ResidentVec<DesktopData>,
}

impl DesktopDisplayCollector {
    pub fn new(db: RealmDatabase) -> Result<Self> {
        Ok(Self {
            data: db.resident_vec(())?,
        })
    }
}

impl Collector for DesktopDisplayCollector {
    async fn refresh(&mut self) -> Result<()> {
        let displays = scrap::Display::all()?;
        trace!(count = displays.len(), "Enumerated desktops");

        'next_display: for display in displays.iter() {
            let name = display.name();
            // Update an existing row if we already know this display.
            for resident in self.data.iter() {
                if resident.read().name == name {
                    resident.update(|d| {
                        d.width = display.width() as u32;
                        d.height = display.height() as u32;
                        d.primary = display.is_primary();
                        d.scale_factor = display.scale();
                        Ok(())
                    })?;
                    continue 'next_display;
                }
            }

            self.data.push(DesktopData {
                name,
                width: display.width() as u32,
                height: display.height() as u32,
                primary: display.is_primary(),
                scale_factor: display.scale(),
                ..Default::default()
            })?;
        }

        Ok(())
    }
}
