//! All connections to a server instance must be authenticated with a group
//! using clientAuth certificates.

use anyhow::Result;
use config::GroupConfig;
use native_db::*;
use native_model::{Model, native_model};
use regex::Regex;
use sandpolis_core::{ClusterId, GroupName};
use sandpolis_database::{Data, DataIdentifier, DataView, DatabaseLayer};
use sandpolis_instance::InstanceLayer;
use sandpolis_macros::Data;
use serde::{Deserialize, Serialize};
use std::fmt::Display;
use std::str::FromStr;
use std::{ops::Deref, path::Path};
use tracing::{debug, info};
use validator::{Validate, ValidationErrors};
use x509_parser::prelude::{FromDer, GeneralName};
use x509_parser::prelude::{ParsedExtension, X509Certificate};

pub mod config;
pub mod messages;
#[cfg(feature = "server")]
pub mod server;

#[derive(Serialize, Deserialize, Clone, Default, Data)]
#[native_model(id = 17, version = 1)]
#[native_db]
pub struct GroupLayerData {
    #[primary_key]
    pub _id: DataIdentifier,

    pub client: Option<GroupClientCert>,
}

#[derive(Clone)]
pub struct GroupLayer {
    database: DatabaseLayer,
}

impl GroupLayer {
    pub fn new(
        config: GroupConfig,
        database: DatabaseLayer,
        instance: InstanceLayer,
    ) -> Result<Self> {
        let groups: DataView<GroupData> = data.collection("/groups")?;

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
            let ca = group.insert_document(
                "ca",
                GroupCaCert::new(
                    instance_layer.data.value().cluster_id,
                    group.data.name.clone(),
                )?,
            )?;

            #[cfg(feature = "server")]
            group.insert_document(
                "server",
                ca.data
                    .server_cert(instance_layer.data.value().instance_id)?,
            )?;
        }

        Ok(Self { groups, data })
    }
}

/// A group is a set of clients and agents that can interact. Each group has a
/// global CA certificate that signs certificates used to connect to the server.
///
/// All servers have a default group called "default".
#[derive(Serialize, Deserialize, Validate, Debug, Clone, Data)]
#[native_model(id = 18, version = 1)]
#[native_db]
pub struct GroupData {
    #[primary_key]
    pub _id: DataIdentifier,

    pub name: GroupName,
    pub owner: String,
}

/// The group's global CA cert. These have a lifetime of 100 years.
#[derive(Serialize, Deserialize, Debug)]
pub struct GroupCaCert {
    pub name: GroupName,
    pub cert: String,
    pub key: String,
}

/// Each server in the cluster gets its own server certificate. These have a
/// lifetime 1 year.
#[derive(Serialize, Deserialize, Debug)]
pub struct GroupServerCert {
    pub cert: String,
    pub key: String,
}

impl GroupServerCert {
    pub fn subject_name(&self) -> Result<String> {
        let pem = pem::parse(&self.cert.as_bytes())?;
        for ext in X509Certificate::from_der(pem.contents())?
            .1
            .iter_extensions()
        {
            match ext.parsed_extension() {
                ParsedExtension::SubjectAlternativeName(san) => {
                    for name in &san.general_names {
                        match name {
                            GeneralName::DNSName(s) => return Ok(s.to_string()),
                            _ => {}
                        }
                    }
                }
                _ => {}
            }
        }

        todo!()
    }
}

/// A _client_ certificate (not as in "client" instance) used to authenticate
/// with a server instance.
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
        // TODO PEM instead of json
        Ok(serde_json::from_slice(&std::fs::read(path)?)?)
    }

    /// Write the certificate to a file.
    pub fn write<P>(&self, path: P) -> Result<()>
    where
        P: AsRef<Path>,
    {
        std::fs::write(path, serde_json::to_vec(self)?)?;
        Ok(())
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

pub enum GroupPermission {
    /// Right to create groups on the server
    Create,
    /// Right to view all groups on the server
    List,
    /// Right to delete any group
    Delete,
}
