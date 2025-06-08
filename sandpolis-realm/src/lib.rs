//! All connections to a server instance must be authenticated with a realm
//! using clientAuth certificates.

use anyhow::Result;
use config::RealmConfig;
use native_db::*;
use native_model::{Model, native_model};
use regex::Regex;
use sandpolis_core::{ClusterId, RealmName};
use sandpolis_database::{Data, DataIdentifier, DatabaseLayer, Watch};
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

#[derive(Serialize, Deserialize, Clone, Default, PartialEq, Eq, Data)]
#[native_model(id = 17, version = 1)]
#[native_db]
pub struct RealmLayerData {
    #[primary_key]
    pub _id: DataIdentifier,

    pub client: Option<RealmClientCert>,
}

#[derive(Clone)]
pub struct RealmLayer {
    data: Watch<RealmLayerData>,
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
            let db = database.get(None).await?;
            let rw = db.rw_transaction()?;
            if rw
                .get()
                .secondary::<RealmData>(RealmDataKey::name, "default".parse::<RealmName>()?)?
                .is_none()
            {
                rw.insert(RealmData {
                    owner: "admin".to_string(),
                    ..Default::default()
                });
                rw.commit()?;
            }

            #[cfg(feature = "server")]
            let ca = realm.insert_document(
                "ca",
                RealmCaCert::new(
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
            data: Watch::singleton(db)?,
        })
    }
}

/// A realm is a set of clients and agents that can interact. Each realm has a
/// global CA certificate that signs certificates used to connect to the server.
///
/// All servers have a default realm called "default".
#[derive(Serialize, Deserialize, Validate, Debug, Clone, Default, PartialEq, Eq, Data)]
#[native_model(id = 18, version = 1)]
#[native_db]
pub struct RealmData {
    #[primary_key]
    pub _id: DataIdentifier,

    #[secondary_key(unique)]
    pub name: RealmName,
    pub owner: String,
}

/// The realm's global CA cert. These have a lifetime of 100 years.
#[derive(Serialize, Deserialize, Debug)]
pub struct RealmCaCert {
    pub name: RealmName,
    pub cert: String,
    pub key: String,
}

/// Each server in the cluster gets its own server certificate. These have a
/// lifetime 1 year.
#[derive(Serialize, Deserialize, Debug)]
pub struct RealmServerCert {
    pub cert: String,
    pub key: String,
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

/// A _client_ certificate (not as in "client" instance) used to authenticate
/// with a server instance.
#[derive(Serialize, Deserialize, Debug, Clone, PartialEq, Eq)]
pub struct RealmClientCert {
    pub ca: String,
    pub cert: String,
    pub key: String,
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

pub enum RealmPermission {
    /// Right to create realms on the server
    Create,
    /// Right to view all realms on the server
    List,
    /// Right to delete any realm
    Delete,
}
