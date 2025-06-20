// doc_comment! {
//     include_str!("../README.md")
// }

use anyhow::Result;
use config::RealmConfig;
use native_db::ToKey;
use native_model::Model;
use regex::Regex;
use sandpolis_core::{ClusterId, RealmName, UserName};
use sandpolis_database::{Data, DataIdentifier, DatabaseLayer, Resident};
use sandpolis_instance::InstanceLayer;
use sandpolis_macros::data;
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

#[data]
pub struct RealmLayerData {
    pub client: Option<RealmClientCert>,
}

#[derive(Clone)]
pub struct RealmLayer {
    data: Resident<RealmLayerData>,
}

impl RealmLayer {
    pub async fn new(
        config: RealmConfig,
        mut database: DatabaseLayer,
        instance: InstanceLayer,
    ) -> Result<Self> {
        // Create default realm if it doesn't exist
        if database.get(Some("default".parse()?)).await.is_err() {
            debug!("Creating default realm");
            let db = database.get(Some("default".parse()?)).await?;

            let default_realm = RealmData::default();

            #[cfg(feature = "server")]
            let ca = realm.insert_document(
                "ca",
                RealmClusterCert::new(
                    instance_layer.data.value().cluster_id,
                    realm.data.name.clone(),
                )?,
            )?;

            #[cfg(feature = "server")]
            realm.insert_document(
                "server",
                ca.data
                    .server_cert(instance_layer.data.value().instance_id)?,
            )?;

            {
                let rw = db.rw_transaction()?;
                rw.insert(default_realm);
                rw.commit()?;
            }
        }

        // Load all realm databases
        let db = database.get(None).await?;
        let r = db.r_transaction()?;
        for realm in r.scan().primary::<RealmData>()?.all()? {
            let realm = realm?;
            database.add_realm(realm.name);
        }

        // Update client cert if possible
        if let Some(new_cert) = config.certificate()? {
            if let Some(realm) = r
                .get()
                .secondary::<RealmData>(RealmDataKey::name, new_cert.name()?)?
            {
                // Only import if the given certificate is newer than the one
                // already in the database.
                // if new_cert.creation_time()? > old_cert.creation_time()? {
                //     info!(path = %config.certificate.expect("a path was
                // configured").display(), "Importing realm certificate");
                //     data.data.client = Some(new_cert);
                // }
            }
        }

        Ok(Self {
            data: Resident::singleton(db)?,
        })
    }
}

/// A realm is a set of clients and agents that can interact. Each realm has a
/// global CA certificate that signs certificates used to connect to the server.
///
/// All servers have a default realm called "default".
#[derive(Validate)]
#[data]
pub struct RealmData {
    #[secondary_key(unique)]
    pub name: RealmName,
    pub owner: UserName,

    pub cluster_cert: Option<RealmClusterCert>,
}

/// The realm's global CA certificate.
#[derive(Serialize, Deserialize, Debug, Clone, PartialEq, Eq)]
pub struct RealmClusterCert {
    pub name: RealmName,
    /// PEM-encoded certificate
    pub cert: String,
    /// PEM-encoded key
    pub key: Option<String>,
}

/// Each server in the cluster gets its own server certificate.
#[derive(Serialize, Deserialize, Debug, Clone, PartialEq, Eq)]
pub struct RealmServerCert {
    /// PEM-encoded certificate
    pub cert: String,
    /// PEM-encoded key
    pub key: Option<String>,
}

impl RealmServerCert {
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

/// Realm certificate for client instances that can authenticate with a server
/// instance against a particular realm.
#[derive(Serialize, Deserialize, Debug, Clone, PartialEq, Eq)]
pub struct RealmClientCert {
    pub ca: String,
    pub cert: String,
    pub key: Option<String>,
}

impl RealmClientCert {
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
        bundle.extend_from_slice(&self.key.ok_or_else(|| anyhow!("No key"))?.as_bytes());
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

    pub fn name(&self) -> Result<RealmName> {
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

/// Realm certificate for agent instances that can authenticate with a server
/// instance against a particular realm.
#[derive(Serialize, Deserialize, Debug, Clone, PartialEq, Eq)]
pub struct RealmAgentCert {
    pub ca: String,
    pub cert: String,
    pub key: String,
}

pub enum RealmPermission {
    /// Right to create new realms on the server
    Create,
    /// Right to view all realms on the server
    List,
    /// Right to delete any realm
    Delete,
}
