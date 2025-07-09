#![feature(iterator_try_collect)]

// doc_comment! {
//     include_str!("../README.md")
// }

use anyhow::Result;
use anyhow::anyhow;
use anyhow::bail;
use config::RealmConfig;
use native_db::ToKey;
use native_model::Model;
use pem::Pem;
use pem::encode;
use sandpolis_core::{RealmName, UserName};
use sandpolis_database::RealmDatabase;
use sandpolis_database::ResidentVec;
use sandpolis_database::{DatabaseLayer, Resident};
use sandpolis_instance::InstanceLayer;
use sandpolis_macros::data;
use std::collections::HashSet;
use std::fs::File;
use std::io::Write;
use std::path::Path;
use tracing::debug;
use tracing::info;
use validator::Validate;
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
    database: DatabaseLayer,
    data: Resident<RealmLayerData>,
    pub realms: ResidentVec<RealmData>,
}

impl RealmLayer {
    pub async fn new(
        config: RealmConfig,
        database: DatabaseLayer,
        instance: InstanceLayer,
    ) -> Result<Self> {
        debug!("Initializing realm layer");

        let default_realm = database.realm(RealmName::default())?;

        // These records have to be stored in the default realm so we know what
        // other realms exist.
        let realms: ResidentVec<RealmData> = default_realm.resident_vec(())?;

        if realms.len() == 0 {
            realms.push(RealmData::default())?;
        }

        // Preload all realm databases
        // TODO

        #[cfg(feature = "server")]
        {
            for realm in realms.iter() {
                let realm_db = database.realm(realm.read().name.clone())?;

                let rw = realm_db.rw_transaction()?;
                let mut cluster_certs: Vec<RealmClusterCert> =
                    rw.scan().primary()?.all()?.try_collect()?;

                // TODO only GS
                if cluster_certs.len() == 0 {
                    cluster_certs.push(RealmClusterCert::new(
                        instance.cluster_id,
                        realm.read().name.clone(),
                    )?);
                    rw.insert(cluster_certs[0].clone())?;

                    // Write certs in development mode to make testing easier
                    #[cfg(debug_assertions)]
                    {
                        let client_cert = cluster_certs[0].client_cert()?;
                        client_cert.write("/tmp/client.pem")?;
                        info!("Wrote client cert to: /tmp/client.pem");

                        let agent_cert = cluster_certs[0].agent_cert()?;
                        client_cert.write("/tmp/agent.pem")?;
                        info!("Wrote agent cert to: /tmp/agent.pem");
                    }
                }

                // Get or create server cert
                let mut server_certs: Vec<RealmServerCert> =
                    rw.scan().primary()?.all()?.try_collect()?;

                if server_certs.len() == 0 {
                    server_certs.push(cluster_certs[0].server_cert(instance.instance_id)?);
                    rw.insert(server_certs[0].clone())?;
                }

                rw.commit()?;
            }
        }

        // Update client cert if possible
        // if let Some(new_cert) = config.certificate()? {
        //     if let Some(realm) = r
        //         .get()
        //         .secondary::<RealmData>(RealmDataKey::name, new_cert.name()?)?
        //     {
        //         // Only import if the given certificate is newer than the one
        //         // already in the database.
        //         // if new_cert.creation_time()? > old_cert.creation_time()? {
        //         //     info!(path = %config.certificate.expect("a path was
        //         // configured").display(), "Importing realm certificate");
        //         //     data.data.client = Some(new_cert);
        //         // }
        //     }
        // }

        Ok(Self {
            database,
            data: default_realm.resident(())?,
            realms,
        })
    }

    pub fn realm(&self, name: RealmName) -> Result<RealmDatabase> {
        // Don't allow this method to create realms that don't already exist
        for realm in self.realms.iter() {
            if realm.read().name == name {
                return Ok(self.database.realm(name)?);
            }
        }
        bail!("Realm does not exist");
    }
}

/// A realm is a set of clients and agents that can interact. Each realm has a
/// global CA certificate that signs certificates used to connect to the server.
///
/// All servers have a default realm called "default". All `RealmData` entries
/// are stored within this realm.
#[derive(Validate)]
#[data]
pub struct RealmData {
    #[secondary_key(unique)]
    pub name: RealmName,
    pub owner: UserName,
}

/// The realm's global CA certificate.
#[data]
pub struct RealmClusterCert {
    pub name: RealmName,
    pub cert: Vec<u8>,
    pub key: Option<Vec<u8>>,
}

/// Each server in the cluster gets its own server certificate.
#[data(instance)]
pub struct RealmServerCert {
    pub cert: Vec<u8>,
    pub key: Option<Vec<u8>>,
}

impl RealmServerCert {
    pub fn subject_name(&self) -> Result<String> {
        for ext in X509Certificate::from_der(&self.cert)?.1.iter_extensions() {
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
#[data]
pub struct RealmClientCert {
    pub ca: Vec<u8>,
    pub cert: Vec<u8>,
    pub key: Option<Vec<u8>>,
}

impl RealmClientCert {
    /// Read the certificate from a file.
    pub fn read<P>(path: P) -> Result<Self>
    where
        P: AsRef<Path>,
    {
        let mut cert = Self::default();
        let file = pem::parse_many(&std::fs::read(path)?)?;

        if file.len() < 2 || file.len() > 3 {
            bail!("Invalid realm certificate");
        }

        // Duplicates are not allowed
        if file
            .iter()
            .map(|item| item.tag())
            .collect::<HashSet<_>>()
            .len()
            != file.len()
        {
            bail!("Invalid realm certificate");
        }

        for item in file {
            match item.tag() {
                "CLUSTER CERTIFICATE" => {
                    cert.ca = item.into_contents();
                }
                "CLIENT CERTIFICATE" => {
                    cert.cert = item.into_contents();
                }
                "CLIENT KEY" => {
                    cert.key = Some(item.into_contents());
                }
                _ => bail!("Invalid realm certificate"),
            }
        }

        assert!(!cert.ca.is_empty());
        assert!(!cert.cert.is_empty());
        Ok(cert)
    }

    /// Write the certificate to a file.
    pub fn write<P>(&self, path: P) -> Result<()>
    where
        P: AsRef<Path>,
    {
        let mut file = File::create(path)?;

        file.write_all(encode(&Pem::new("CLUSTER CERTIFICATE", self.ca.clone())).as_bytes())?;
        file.write_all(encode(&Pem::new("CLIENT CERTIFICATE", self.cert.clone())).as_bytes())?;

        if let Some(key) = self.key.clone() {
            file.write_all(encode(&Pem::new("CLIENT KEY", key)).as_bytes())?;
        }
        Ok(())
    }

    pub fn ca(&self) -> Result<reqwest::Certificate> {
        Ok(reqwest::Certificate::from_pem(&self.ca)?)
    }

    pub fn identity(&self) -> Result<reqwest::Identity> {
        // Combine cert and key together
        let mut bundle = Vec::new();
        bundle.extend_from_slice(&self.cert);
        bundle.extend_from_slice(self.key.as_ref().ok_or_else(|| anyhow!("No key"))?);
        Ok(reqwest::Identity::from_pem(&bundle)?)
    }

    /// Return when the certificate was generated.
    pub fn creation_time(&self) -> Result<i64> {
        Ok(X509Certificate::from_der(&self.cert)?
            .1
            .validity
            .not_before
            .timestamp())
    }

    pub fn name(&self) -> Result<RealmName> {
        let name = X509Certificate::from_der(&self.cert)?
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

#[cfg(test)]
mod test_client_cert {
    use super::*;

    #[test]
    fn test_read_write() -> Result<()> {
        let mut temp_file = tempfile::NamedTempFile::new()?;

        let cert = RealmClientCert {
            ca: "doesn't have to be a valid cert".as_bytes().to_vec(),
            cert: "doesn't have to be a valid cert".as_bytes().to_vec(),
            key: Some("doesn't have to be a valid key".as_bytes().to_vec()),
            ..Default::default()
        };

        cert.write(temp_file.path())?;

        assert_eq!(cert, RealmClientCert::read(temp_file.path())?);
        Ok(())
    }
}

/// Realm certificate for agent instances that can authenticate with a server
/// instance against a particular realm.
#[data]
pub struct RealmAgentCert {
    pub ca: Vec<u8>,
    pub cert: Vec<u8>,
    pub key: Option<Vec<u8>>,
}

pub enum RealmPermission {
    /// Right to create new realms on the server
    Create,
    /// Right to view all realms on the server
    List,
    /// Right to delete any realm
    Delete,
}
