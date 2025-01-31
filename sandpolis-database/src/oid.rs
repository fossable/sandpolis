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
