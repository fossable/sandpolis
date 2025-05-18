use crate::oid::Oid;
use anyhow::{Result, anyhow};
use chrono::{DateTime, Utc};
use native_db::{Models, ToKey};
use serde::de::DeserializeOwned;
use serde::{Deserialize, Serialize};
use sled::transaction::ConflictableTransactionError::Abort;
use std::ops::Range;
use std::path::Path;
use std::{marker::PhantomData, sync::Arc};
use tempfile::{TempDir, tempdir};
use tracing::{debug, trace};

pub mod config;
pub mod oid;

#[derive(Clone)]
pub struct Database(pub Arc<native_db::Database<'static>>);

impl Database {
    /// Initialize a new memory-only database.
    pub fn new_ephemeral(models: &'static Models) -> Result<Self> {
        debug!("Initializing ephemeral database");
        Ok(Self(Arc::new(
            native_db::Builder::new().create_in_memory(models)?,
        )))
    }

    /// Initialize a new persistent database from the given directory.
    pub fn new<P>(models: &'static Models, path: P) -> Result<Self>
    where
        P: AsRef<Path>,
    {
        let path = path.as_ref();

        debug!(path = %path.display(), "Initializing persistent database");
        Ok(Self(Arc::new(
            native_db::Builder::new().create(models, path)?,
        )))
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
                // TODO insert
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
                // TODO insert
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
