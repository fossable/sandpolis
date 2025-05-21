use crate::oid::Oid;
use anyhow::{Result, anyhow, bail};
use chrono::{DateTime, Utc};
use native_db::{Models, ToKey};
use serde::de::DeserializeOwned;
use serde::{Deserialize, Serialize};
use sled::transaction::ConflictableTransactionError::Abort;
use std::ops::Range;
use std::path::Path;
use std::{marker::PhantomData, sync::Arc};
use tracing::{debug, trace};

pub mod config;
pub mod oid;

pub struct DatabaseBuilder {
    models: &'static Models,
    groups: Vec<(String, native_db::Database<'static>)>,
}

impl DatabaseBuilder {
    pub fn new(models: &'static Models) -> Self {
        Self {
            models,
            // Don't bother with a map because groups are few
            groups: Vec::with_capacity(1),
        }
    }

    /// Create a new ephemeral database for the given group name.
    pub fn add_ephemeral(mut self, group: impl ToString) -> Result<Self> {
        let name = group.to_string();

        // Check for duplicates
        for (n, _) in self.groups.iter() {
            if name == *n {
                bail!("Duplicate group");
            }
        }

        debug!(group = name, "Initializing ephemeral database");
        self.groups.push((
            name,
            native_db::Builder::new().create_in_memory(self.models)?,
        ));

        Ok(self)
    }

    /// Load or create a new persistent database for the given group name.
    pub fn add_persistent<P>(mut self, group: impl ToString, path: P) -> Result<Self>
    where
        P: AsRef<Path>,
    {
        let name = group.to_string();
        let path = path.as_ref();

        // Check for duplicates
        for (n, _) in self.groups.iter() {
            if name == *n {
                bail!("Duplicate group");
            }
        }

        debug!(group = name, path = %path.display(), "Initializing persistent database");
        self.groups
            .push((name, native_db::Builder::new().create(self.models, path)?));

        Ok(self)
    }

    pub fn build(self) -> Database {
        Database(Arc::new(self.groups))
    }
}

#[derive(Clone)]
pub struct Database(Arc<Vec<(String, native_db::Database<'static>)>>);

impl Database {}

// TODO Eq based only on inner?
#[derive(Serialize, Deserialize, Eq, PartialEq, Debug, Clone, Hash)]
pub enum DbTimestamp {
    Latest(DateTime<Utc>),
    Previous(DateTime<Utc>),
}

impl ToKey for DbTimestamp {
    fn to_key(&self) -> native_db::Key {
        native_db::Key::new(
            match self {
                DbTimestamp::Latest(dt) => dt,
                DbTimestamp::Previous(dt) => dt,
            }
            .timestamp_millis()
            .to_be_bytes()
            .to_vec(),
        )
    }

    fn key_names() -> Vec<String> {
        vec!["DbTimestamp".to_string()]
    }
}
