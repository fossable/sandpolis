use crate::oid::Oid;
use anyhow::{anyhow, Result};
use sandpolis_instance::InstanceData;
use serde::de::DeserializeOwned;
use serde::Serialize;
use sled::transaction::ConflictableTransactionError::Abort;
use std::ops::Range;
use std::path::Path;
use std::{marker::PhantomData, sync::Arc};
use tempfile::{tempdir, TempDir};
use tracing::{debug, trace};

pub mod config;
pub mod oid;

#[derive(Clone)]
pub struct Database(sled::Db);

impl Database {
    /// Initialize a new database from the given directory.
    pub fn new<P>(path: P) -> Result<Self>
    where
        P: AsRef<Path>,
    {
        let path = path.as_ref();

        debug!(path = %path.display(), "Initializing database");
        Ok(Self(sled::Config::new().path(path).open()?))
    }

    pub fn get_document<T>(&self, oid: impl TryInto<Oid>) -> Result<Option<Document<T>>>
    where
        T: Serialize + DeserializeOwned,
    {
        let oid = oid.try_into().map_err(|_| anyhow!("Invalid OID"))?;
        let db = self.0.open_tree("default")?;

        Ok(if let Some(data) = db.get(&oid)? {
            Some(Document {
                db: db.clone(),
                data: serde_cbor::from_slice::<T>(&data)?,
                oid,
            })
        } else {
            None
        })
    }

    pub fn document<T>(&self, oid: impl TryInto<Oid>) -> Result<Document<T>>
    where
        T: Serialize + DeserializeOwned + Default,
    {
        let oid = oid.try_into().map_err(|_| anyhow!("Invalid OID"))?;
        let db = self.0.open_tree("default")?;

        Ok(Document {
            data: if let Some(data) = db.get(&oid)? {
                serde_cbor::from_slice::<T>(&data)?
            } else {
                T::default()
            },
            db,
            oid,
        })
    }

    pub fn collection<T>(&self, oid: impl TryInto<Oid>) -> Result<Collection<T>>
    where
        T: Serialize + DeserializeOwned,
    {
        let oid = oid.try_into().map_err(|_| anyhow!("Invalid OID"))?;
        todo!()
    }

    pub fn metadata(&self) -> Result<InstanceData> {
        Ok(self.document("metadata")?.data)
    }
}

#[derive(Clone)]
pub struct Collection<T>
where
    T: Serialize + DeserializeOwned,
{
    db: sled::Tree,
    oid: Oid,
    data: PhantomData<T>,
}

impl<T: Serialize + DeserializeOwned + std::fmt::Debug> Collection<T> {
    pub fn get_document(&self, oid: impl TryInto<Oid>) -> Result<Option<Document<T>>> {
        let oid = self.oid.extend(oid)?;

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

    pub fn documents(&self) -> impl Iterator<Item = Result<Document<T>>> + use<'_, T> {
        trace!(oid = %self.oid, "Querying collection");
        let mut start = self.oid.0.clone();
        start.push('/' as u8);
        let mut end = start.clone();
        end.push(0xff);

        self.db
            .range::<&[u8], Range<&[u8]>>(&start..&end)
            .map(|r| match r {
                Ok((key, value)) => match (key.try_into(), serde_cbor::from_slice::<T>(&value)) {
                    (Ok(oid), Ok(data)) => {
                        trace!(oid = %oid, "Yielding document");
                        Ok(Document {
                            db: self.db.clone(),
                            oid,
                            data,
                        })
                    }
                    (Ok(_), Err(_)) => todo!(),
                    (Err(_), Ok(_)) => todo!(),
                    (Err(_), Err(_)) => todo!(),
                },
                Err(_) => todo!(),
            })
    }

    pub fn collection<U>(&self, oid: impl TryInto<Oid>) -> Result<Collection<U>>
    where
        U: Serialize + DeserializeOwned,
    {
        Ok(Collection {
            db: self.db.clone(),
            oid: self.oid.extend(oid)?,
            data: PhantomData {},
        })
    }

    pub fn insert_document(&self, oid: impl TryInto<Oid>, data: T) -> Result<Document<T>> {
        let oid = self.oid.extend(oid)?;
        let d = serde_cbor::to_vec(&data)?;

        trace!(oid = %oid, data = ?data, "Inserting new document");
        self.db
            .transaction(|tx_db| {
                if tx_db.get(&oid)?.is_some() {
                    return Err(Abort(anyhow::anyhow!("Already exists")));
                }

                tx_db.insert(oid.clone(), d.clone())?;
                Ok(())
            })
            .map_err(|_| anyhow::anyhow!(""))?;

        Ok(Document {
            oid,
            data,
            db: self.db.clone(),
        })
    }
}

impl<T: Serialize + DeserializeOwned + Default> Collection<T> {
    pub fn document(&self, oid: impl TryInto<Oid>) -> Result<Document<T>> {
        let oid = self.oid.extend(oid)?;

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
    T: Serialize + DeserializeOwned,
{
    pub db: sled::Tree,
    pub oid: Oid,
    pub data: T, // TODO impl AsRef?
}

impl<T: Serialize + DeserializeOwned> Document<T> {
    // pub fn update<U>(&mut self, update: )

    pub fn get_document<U>(&self, oid: impl TryInto<Oid>) -> Result<Option<Document<U>>>
    where
        U: Serialize + DeserializeOwned,
    {
        let oid = self.oid.extend(oid)?;

        Ok(if let Some(data) = self.db.get(&oid)? {
            Some(Document {
                db: self.db.clone(),
                data: serde_cbor::from_slice::<U>(&data)?,
                oid,
            })
        } else {
            None
        })
    }

    pub fn document<U>(&self, oid: impl TryInto<Oid>) -> Result<Document<U>>
    where
        U: Serialize + DeserializeOwned + Default,
    {
        let oid = self.oid.extend(oid)?;

        Ok(Document {
            db: self.db.clone(),
            data: if let Some(data) = self.db.get(&oid)? {
                trace!(oid = %oid, "Loading document");
                serde_cbor::from_slice::<U>(&data)?
            } else {
                trace!(oid = %oid, "Creating new document");
                U::default()
            },
            oid,
        })
    }

    pub fn insert_document<U>(&self, oid: impl TryInto<Oid>, data: U) -> Result<Document<U>>
    where
        U: Serialize + DeserializeOwned + std::fmt::Debug,
    {
        let oid = self.oid.extend(oid)?;
        let d = serde_cbor::to_vec(&data)?;

        trace!(oid = %oid, data = ?data, "Inserting new document");
        self.db
            .transaction(|tx_db| {
                if tx_db.get(&oid)?.is_some() {
                    return Err(Abort(anyhow::anyhow!("Already exists")));
                }

                tx_db.insert(oid.clone(), d.clone())?;
                Ok(())
            })
            .map_err(|_| anyhow::anyhow!(""))?;

        Ok(Document {
            oid,
            data,
            db: self.db.clone(),
        })
    }

    pub fn collection<U>(&self, oid: impl TryInto<Oid>) -> Result<Collection<U>>
    where
        U: Serialize + DeserializeOwned,
    {
        Ok(Collection {
            db: self.db.clone(),
            oid: self.oid.extend(oid)?,
            data: PhantomData {},
        })
    }
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

/// Holds a database that's deleted once we're done with it.
#[derive(Clone)]
pub struct TemporaryDatabase {
    pub db: Database,
    _path: Arc<TempDir>,
}

impl TemporaryDatabase {
    pub fn new() -> Result<Self> {
        let path = tempdir()?;
        Ok(Self {
            db: Database::new(&path)?,
            _path: Arc::new(path),
        })
    }

    /// Create a new temporary database and seed it with the given initializer.
    pub fn new_with<F>(seed: F) -> Result<Self>
    where
        F: Fn(&Database) -> Result<()>,
    {
        let temp = Self::new()?;
        seed(&temp.db)?;
        Ok(temp)
    }
}
