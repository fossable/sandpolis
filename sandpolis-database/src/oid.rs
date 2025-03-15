use anyhow::{Result, anyhow};
use sled::IVec;
use std::fmt::Display;
use std::fmt::Write;

/// Locates a `Document` or `Collection` in the instance state tree.
///
/// #### Path
///
/// The OID path is a sequence of `/` separated strings that describe how to
/// reach the corresponding node from the root node.
///
/// Elements of the path are called _components_ which may consist of any number
/// of alphanumeric characters and underscores. If a component equals the
/// wildcard character (`*`), then the OID corresponds to all possible values of
/// that component and is known as a _generic_ OID. If an OID is not generic,
/// then it's _concrete_.
///
/// #### Namespace
///
/// OIDs have a namespace string that identifies the module that provides the
/// OID. This allows modules to define OIDs without the possibility of
/// collisions. The namespace string must equal the name of the module that
/// defines an OID.
///
/// Namespace notation is to prefix the namespace string and a `:`, similar to
/// the protocol section of a URI:
///
/// ```
/// com.sandpolis.plugin.example:/profile/*/example
/// ```
///
/// #### Temporal Selector
///
/// In order to select historic values of an attribute, concrete OIDs may
/// include a timestamp range selector or an index selector.
///
/// ##### Timestamp Selector
///
/// To select all values within an arbitrary timestamp range, specify the
/// inclusive start and end epoch timestamps separated by a `..` in parenthesis.
/// If either timestamp is omitted, then the range is extended to the most
/// extreme value possible.
///
/// ```
/// /profile/ba4412ea-1ec6-4e76-be78-3849d2196b52/example(1628216870..1628216880)
/// ```
///
/// ##### Index Selector
///
/// To select an arbitrary amount of values, specify inclusive start and end
/// indicies separated by a `..` in square brackets. If either index is omitted,
/// then the range is extended to the most extreme value possible. Index 0 is
/// the oldest value.
///
/// ```
/// /profile/ba4412ea-1ec6-4e76-be78-3849d2196b52/example[2..7]
/// ```
///
/// To select one value, omit the range specifier entirely:
///
/// ```
/// /profile/ba4412ea-1ec6-4e76-be78-3849d2196b52/example[1]
/// ```
#[derive(Clone)]
pub struct Oid(pub Vec<u8>);

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

impl TryFrom<IVec> for Oid {
    type Error = anyhow::Error;

    fn try_from(value: IVec) -> Result<Self> {
        // TODO validate
        Ok(Oid(value.to_vec()))
    }
}

impl AsRef<[u8]> for Oid {
    fn as_ref(&self) -> &[u8] {
        &self.0
    }
}

impl Into<IVec> for Oid {
    fn into(self) -> IVec {
        self.as_ref().into()
    }
}
