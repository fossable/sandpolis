//! All connections to a server instance must be authenticated with a group using
//! clientAuth certificates.

use anyhow::Result;
use config::GroupConfig;
use regex::Regex;
use sandpolis_database::{Collection, Document};
use sandpolis_instance::{ClusterId, InstanceLayer};
use serde::{Deserialize, Serialize};
use std::fmt::Display;
use std::str::FromStr;
use std::{ops::Deref, path::Path};
use tracing::{debug, info};
use validator::{Validate, ValidationErrors};
use x509_parser::prelude::FromDer;
use x509_parser::prelude::X509Certificate;

pub mod config;
pub mod messages;
#[cfg(feature = "server")]
pub mod server;

#[derive(Serialize, Deserialize, Clone, Default)]
pub struct GroupLayerData {
    pub client: Option<GroupClientCert>,
}

#[derive(Clone)]
pub struct GroupLayer {
    pub data: Document<GroupLayerData>,
    pub groups: Collection<GroupData>,
}

impl GroupLayer {
    pub fn new(
        config: GroupConfig,
        mut data: Document<GroupLayerData>,
        instance: InstanceLayer,
    ) -> Result<Self> {
        let groups: Collection<GroupData> = data.collection("/groups")?;

        if let Some(new_cert) = config.certificate()? {
            if let Some(old_cert) = data.data.client.as_ref() {
                // Only import if the given certificate is newer than the one already
                // in the database.
                if new_cert.creation_time()? > old_cert.creation_time()? {
                    info!(path = %config.certificate.expect("a path was configured").display(), "Importing group certificate");
                    data.data.client = Some(new_cert);
                }
            } else {
                data.data.client = Some(new_cert);
            }
        }

        // Create default group
        if groups
            .documents()
            .filter_map(|group| group.ok())
            .find(|group| *group.data.name == "default")
            .is_none()
        {
            debug!("Creating default group");
            let group = groups.insert_document(
                "default",
                GroupData {
                    name: "default".parse().expect("valid group name"),
                    owner: "admin".to_string(),
                },
            )?;

            #[cfg(feature = "server")]
            group.insert_document(
                "ca",
                GroupCaCert::new(instance.cluster_id, group.data.name.clone())?,
            )?;
        }

        Ok(Self { groups, data })
    }
}

/// A group is a set of clients and agents that can interact. Each group has a
/// CA certificate that signs certificates used to connect to the server.
///
/// All servers have a default group.
#[derive(Serialize, Deserialize, Validate, Debug, Clone)]
pub struct GroupData {
    pub name: GroupName,
    pub owner: String,
}

/// Groups have unique names and are shared across the entire cluster. Group
/// names cannot be changed after they are created.
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct GroupName(String);

impl Display for GroupName {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(&self.0)
    }
}

impl Deref for GroupName {
    type Target = String;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl FromStr for GroupName {
    type Err = anyhow::Error;

    fn from_str(s: &str) -> Result<Self> {
        let name = GroupName(s.to_string());
        name.validate()?;
        Ok(name)
    }
}

impl Validate for GroupName {
    fn validate(&self) -> Result<(), ValidationErrors> {
        if Regex::new("^[a-z0-9]{4,32}$").unwrap().is_match(&self.0) {
            Ok(())
        } else {
            Err(ValidationErrors::new())
        }
    }
}

#[cfg(test)]
mod test_group_name {
    use super::*;

    #[test]
    fn test_valid() {
        assert!("test".parse::<GroupName>().is_ok());
        assert!("default".parse::<GroupName>().is_ok());
    }

    #[test]
    fn test_invalid() {
        assert!("t".parse::<GroupName>().is_err());
        assert!("".parse::<GroupName>().is_err());
        assert!("test*".parse::<GroupName>().is_err());
        assert!("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            .parse::<GroupName>()
            .is_err());
    }
}

#[derive(Serialize, Deserialize, Debug)]
pub struct GroupCaCert {
    pub name: GroupName,
    pub cert: String,
    pub key: String,
}

pub struct GroupServerCert {
    pub cert: String,
    pub key: String,
}

/// A _client_ certificate (not as in "client" instance) used to authenticate with
/// a server instance.
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct GroupClientCert {
    pub ca: String,
    pub cert: String,
    pub key: String,
}

impl GroupClientCert {
    /// Read the certificate from a file.
    pub fn read<P>(path: P) -> Result<Self>
    where
        P: AsRef<Path>,
    {
        Ok(serde_json::from_slice(&std::fs::read(path)?)?)
    }

    pub fn ca(&self) -> Result<reqwest::Certificate> {
        Ok(reqwest::Certificate::from_pem(self.ca.as_bytes())?)
    }

    pub fn identity(&self) -> Result<reqwest::Identity> {
        // Combine cert and key together
        let mut bundle = Vec::new();
        bundle.extend_from_slice(&self.cert.as_bytes());
        bundle.extend_from_slice(&self.key.as_bytes());
        Ok(reqwest::Identity::from_pem(&bundle)?)
    }

    /// Return when the certificate was generated.
    pub fn creation_time(&self) -> Result<i64> {
        let pem = pem::parse(&self.cert.as_bytes())?;
        Ok(X509Certificate::from_der(pem.contents())?
            .1
            .validity
            .not_before
            .timestamp())
    }

    pub fn name(&self) -> Result<GroupName> {
        let pem = pem::parse(&self.cert.as_bytes())?;
        let name = X509Certificate::from_der(pem.contents())?
            .1
            .subject()
            .iter_common_name()
            .next()
            .ok_or_else(|| anyhow::anyhow!("no common name"))?
            .to_owned()
            .as_str()
            .map_err(|_| anyhow::anyhow!("invalid common name"))?
            .parse()?;

        Ok(name)
    }
}
