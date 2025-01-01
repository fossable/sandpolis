use anyhow::{bail, Result};
use std::{path::Path, sync::Arc};
use tracing::debug;

use super::InstanceId;
use super::{Instance, InstanceData, InstanceType};

#[derive(Clone)]
pub struct Database {
    db: sled::Db,
    this: Arc<Instance>,
}

impl Database {
    pub fn new<P>(path: P) -> Result<Self>
    where
        P: AsRef<Path>,
    {
        let db = sled::Config::new().path(path.as_ref()).open()?;

        // Query for the local instance
        let local: InstanceData = if let Some(local) = db.get("local")? {
            let local = serde_json::from_slice(&local)?;
            debug!(local = ?local, "Restoring local instance metadata");
            local
        } else {
            let local = InstanceData::new();
            debug!(local = ?local, "Creating new local instance metadata");
            db.insert("local", serde_json::to_vec(&local)?)?;
            local
        };

        Ok(Self {
            this: Arc::new(Instance {
                db: db.open_tree(local.id)?,
                data: local,
            }),
            db,
        })
    }

    pub fn instance(&self, id: impl Into<InstanceId>) -> Result<InstanceData> {
        let id = id.into();
        let db = self.db.open_tree(id)?;

        Ok(if let Some(data) = db.get("instance")? {
            serde_json::from_slice::<InstanceData>(&data)?
        } else {
            let instance = InstanceData::new();
            db.insert("local", serde_json::to_vec(&instance)?)?;
            instance
        })
    }
}
