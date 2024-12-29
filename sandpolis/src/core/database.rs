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

use super::Instance;
use super::InstanceId;

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct LocalMetadata {
    pub id: InstanceId,
    pub os_info: os_info::Info,
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
        let local: LocalMetadata = if let Some(local) = db.get("local")? {
            serde_json::from_slice(&local)?
        } else {
            todo!()
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
