use anyhow::{bail, Result};
use serde::de::DeserializeOwned;
use serde::{Deserialize, Serialize};
use std::marker::PhantomData;
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

    pub fn instance(&self, id: impl Into<InstanceId>) -> Result<Oid> {
        Ok(Oid {
            db: self.db.open_tree(id.into())?,
            path: vec!['/' as u8],
        })
    }
}

/// Identifies an object in the database.
#[derive(Clone)]
pub struct Oid(Vec<u8>);

impl Oid {
    pub fn extend(mut self, extension: &str) -> Result<Oid> {
        self.path.push('/' as u8);
        self.path.extend_from_slice(extension.as_bytes());
        Ok(self)
    }

    pub fn extend_id(mut self, id: u64) -> Result<Oid<T>> {
        // Prohibit multiple IDs
        for byte in self.path.iter().rev() {
            if *byte == ':' as u8 {
                // TODO
            }
        }

        self.path.push(':' as u8);
        self.path.extend_from_slice(&id.to_be_bytes());
        Ok(self)
    }

    pub fn history(&self) -> Oid {
        let mut path = "/history".as_bytes().to_vec();
        path.extend_from_slice(&self.0);
        Oid(path)
    }
}

impl From<u32> for Oid {
    fn from(value: u32) -> Self {
        Oid(value.to_be_bytes().to_vec())
    }
}

impl AsRef<[u8]> for Oid {
    fn as_ref(&self) -> &[u8] {
        &self.0
    }
}

#[derive(Clone)]
pub struct Collection<T>
where
    T: Serialize + DeserializeOwned + Clone,
{
    db: sled::Tree,
    oid: Oid,
    data: PhantomData<T>,
}

impl<T: Serialize + DeserializeOwned + Clone> Collection<T> {
    pub fn get_document<I>(&self, id: I) -> Result<Option<Document<T>>>
    where
        I: Into<Oid>,
    {
        let oid = self.oid.extend_id(id.into())?;

        Ok(if let Some(data) = self.db.get(&oid)? {
            Some(Document {
                db: self.db.clone(),
                data: serde_cbor::from_slice::<T>(&data)?,
                oid,
            })
        } else {
            None
        })
    }
}

impl<T: Serialize + DeserializeOwned + Clone + Default> Collection<T> {
    pub fn document<I>(&self, id: I) -> Result<Document<T>>
    where
        I: Into<Oid>,
    {
        let oid = self.oid.extend_id(id.into())?;

        Ok(Document {
            db: self.db.clone(),
            data: if let Some(data) = self.db.get(&oid)? {
                serde_cbor::from_slice::<T>(&data)?
            } else {
                T::default()
            },
            oid,
        })
    }
}

#[derive(Clone)]
pub struct Document<T>
where
    T: Serialize + DeserializeOwned + Clone,
{
    db: sled::Tree,
    oid: Oid,
    data: T,
}

impl<T: Serialize + DeserializeOwned + Clone> Document<T> {
    pub fn mutate<F>(&mut self, mutator: F) -> Result<()>
    where
        F: Fn(&mut T) -> Result<()>,
    {
        // Create a copy so we can tell what changed
        let mut object = self.data.clone();
        mutator(&mut object)?;

        // TODO detect changes and update history as part of a transaction

        self.db.insert(&self.oid, serde_cbor::to_vec(&object)?)?;
        Ok(())
    }
}
