use anyhow::{anyhow, Result};
use serde::de::DeserializeOwned;
use serde::{Deserialize, Serialize};
use std::fmt::{Display, Write};
use std::marker::PhantomData;
use std::{path::Path, sync::Arc};
use tracing::{debug, trace};

use super::InstanceId;

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

    pub fn instance(&self, id: impl Into<InstanceId>) -> Result<Oid> {
        // Ok(Oid {
        //     db: self.db.open_tree(id.into())?,
        //     path: vec!['/' as u8],
        // })
        todo!()
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
                f.write_char(*b as char)?;

                if ':' as u8 == *b {
                    // The next 8 bytes are the timestamp
                    let mut timestamp = 0u64;
                    for i in 0..8 {
                        if let Some(byte) = iter.next() {
                            timestamp |= (*byte as u64) << (i * 8);
                        } else {
                            return Err(std::fmt::Error);
                        }
                    }
                    f.write_str(&format!("{}", timestamp))?;
                }
            } else {
                break;
            }
        }
        Ok(())
    }
}

#[cfg(test)]
mod test_display {
    use super::Oid;

    #[test]
    fn test_format_good() {
        assert_eq!(Oid("/a".as_bytes().to_vec()).to_string(), "/a");
    }
}

impl Oid {
    pub fn extend(&self, oid: impl TryInto<Oid>) -> Result<Oid> {
        let oid = oid.try_into().map_err(|_| anyhow!("Invalid OID"))?;
        let mut path = self.0.clone();

        path.push('/' as u8);
        path.extend_from_slice(&oid.0);
        Ok(Oid(path))
    }

    /// Add or replace the timestamp.
    pub fn timestamp(&self, timestamp: u64) -> Result<Oid> {
        // TODO overwrite timestamp if one exists
        // for byte in self.path.iter().rev() {
        //     if *byte == ':' as u8 {
        //         // TODO
        //     }
        // }

        // self.path.push(':' as u8);
        // self.path.extend_from_slice(&id.to_be_bytes());
        todo!()
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
        // TODO validate
        Ok(Oid(value.as_bytes().to_vec()))
    }
}

impl TryFrom<&String> for Oid {
    type Error = anyhow::Error;

    fn try_from(value: &String) -> Result<Self> {
        // TODO validate
        Ok(Oid(value.as_bytes().to_vec()))
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
    T: Serialize + DeserializeOwned,
{
    db: sled::Tree,
    oid: Oid,
    data: PhantomData<T>,
}

impl<T: Serialize + DeserializeOwned> Collection<T> {
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

    pub fn documents(&self) {
        self.db
            .range::<&Oid, std::ops::Range<&Oid>>(&self.oid..&self.oid);
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
    pub data: T,
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
                serde_cbor::from_slice::<U>(&data)?
            } else {
                trace!(oid = %oid, "Creating new document");
                U::default()
            },
            oid,
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
