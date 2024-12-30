use anyhow::{bail, Result};
use serde::{Deserialize, Serialize};
use std::{
    collections::HashMap,
    path::{Path, PathBuf},
    process::{Child, Command, Stdio},
    sync::{Arc, Mutex},
    thread::sleep,
    time::Duration,
};
use tracing::debug;

use super::InstanceId;
use super::{Instance, InstanceType};

#[derive(Serialize, Deserialize, Clone, Debug)]
struct DatabaseMetadata {
    pub id: InstanceId,
    pub os_info: os_info::Info,
}

impl DatabaseMetadata {
    pub fn new() -> Self {
        DatabaseMetadata {
            id: InstanceId::new(&[
                #[cfg(feature = "server")]
                InstanceType::Server,
                #[cfg(feature = "client")]
                InstanceType::Client,
                #[cfg(feature = "agent")]
                InstanceType::Agent,
            ]),
            os_info: os_info::get(),
        }
    }
}

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
        let local: DatabaseMetadata = if let Some(local) = db.get("local")? {
            let local = serde_json::from_slice(&local)?;
            debug!(local = ?local, "Restoring local instance metadata");
            local
        } else {
            let local = DatabaseMetadata::new();
            debug!(local = ?local, "Creating new local instance metadata");
            db.insert("local", serde_json::to_vec(&local)?)?;
            local
        };

        Ok(Self {
            this: Arc::new(Instance {
                id: local.id,
                db: db.open_tree(local.id)?,
            }),
            db,
        })
    }

    pub fn instance(&self, id: impl Into<InstanceId>) -> Instance {
        let id = id.into();

        Instance {
            id,
            db: self.db.open_tree(id.into_bytes()).unwrap(),
        }
    }
}
