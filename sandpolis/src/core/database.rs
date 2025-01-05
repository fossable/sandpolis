use anyhow::{bail, Result};
use serde::de::DeserializeOwned;
use serde::{Deserialize, Serialize};
use std::fmt::{Display, Write};
use std::marker::PhantomData;
use std::str::FromStr;
use std::{path::Path, sync::Arc};
use tracing::debug;

use super::InstanceId;
use super::{Instance, InstanceData, InstanceType};

#[derive(Clone)]
pub struct Database(sled::Db);

impl Database {
    pub fn new<P>(path: P) -> Result<Self>
    where
        P: AsRef<Path>,
    {
        Ok(Self(sled::Config::new().path(path.as_ref()).open()?))
    }

    pub fn document<T>(&self, oid: impl TryInto<Oid>) -> Result<Document<T>>
    where
        T: Serialize + DeserializeOwned + Clone,
    {
        todo!()
    }

    pub fn instance(&self, id: impl Into<InstanceId>) -> Result<Oid> {
        Ok(Oid {
            db: self.db.open_tree(id.into())?,
            path: vec!['/' as u8],
        })
    }
}

/// Locates a `Document` or `Collection` in the instance state tree.
#[derive(Clone)]
pub struct Oid(Vec<u8>);

impl Display for Oid {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        let mut iter = self.0.iter();
        loop {
            if let Some(b) = iter.next() {
                f.write_char(*b as char);

                if ':' as u8 == *b {
                    // TODO timestamp
                }
            } else {
                break;
            }
        }
        todo!()
    }
}

impl Oid {
    pub fn extend(mut self, extension: &str) -> Result<Oid> {
        self.path.push('/' as u8);
        self.path.extend_from_slice(extension.as_bytes());
        Ok(self)
    }

    pub fn timestamp(mut self, timestamp: u64) -> Result<Oid<T>> {
        // TODO overwrite timestamp if one exists
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

impl TryFrom<&str> for Oid {
    type Error = anyhow::Error;

    fn try_from(value: &str) -> Result<Self> {
        todo!()
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

    pub fn collection<U>(&self) -> Result<Collection<U>>
    where
        U: Serialize + DeserializeOwned + Clone,
    {
        todo!()
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
    pub db: sled::Tree,
    pub oid: Oid,
    pub data: T,
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
