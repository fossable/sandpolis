use anyhow::Result;
use anyhow::anyhow;
use anyhow::bail;
use config::RealmConfig;
#[cfg(feature = "server")]
use headers::{Header, HeaderName, HeaderValue};
use native_db::ToKey;
use native_model::Model;
use pem::Pem;
use pem::encode;
use regex::Regex;
use sandpolis_instance::ClusterId;
use sandpolis_instance::InstanceLayer;
use sandpolis_instance::InstanceType;
use sandpolis_instance::database::RealmDatabase;
use sandpolis_instance::database::ResidentVec;
use sandpolis_instance::database::{DatabaseLayer, Resident};
use sandpolis_macros::data;
use serde::{Deserialize, Serialize};
use std::collections::HashSet;
use std::fmt::Display;
use std::fs::File;
use std::io::Write;
use std::ops::Deref;
use std::path::Path;
use std::str::FromStr;
use std::sync::LazyLock;
use tracing::debug;
use tracing::info;
use validator::{Validate, ValidationError, ValidationErrors};
use x509_parser::asn1_rs::Oid;
use x509_parser::prelude::{FromDer, GeneralName};
use x509_parser::prelude::{ParsedExtension, X509Certificate};

pub mod cli;
pub mod config;
pub mod messages;
#[cfg(feature = "server")]
pub mod server;

static REALM_NAME_REGEX: LazyLock<Regex> =
    LazyLock::new(|| Regex::new("^[a-z0-9]{4,32}$").unwrap());

/// Realms have unique names and are shared across the entire cluster. Realm
/// names cannot be changed after they are created.
#[derive(Serialize, Deserialize, Debug, Clone, PartialEq, Eq, Hash, PartialOrd, Ord)]
pub struct RealmName(String);

impl Default for RealmName {
    fn default() -> Self {
        Self("default".into())
    }
}

impl Display for RealmName {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(&self.0)
    }
}

impl Deref for RealmName {
    type Target = String;

    fn deref(&self) -> &Self::Target {
        &self.0
    }
}

impl FromStr for RealmName {
    type Err = anyhow::Error;

    fn from_str(s: &str) -> Result<Self> {
        let name = RealmName(s.to_string());
        name.validate()?;
        Ok(name)
    }
}

impl Validate for RealmName {
    fn validate(&self) -> Result<(), ValidationErrors> {
        if REALM_NAME_REGEX.is_match(&self.0) {
            Ok(())
        } else {
            Err(ValidationErrors::new())
        }
    }
}

impl ToKey for RealmName {
    fn to_key(&self) -> native_db::Key {
        native_db::Key::new(self.0.as_bytes().to_vec())
    }

    fn key_names() -> Vec<String> {
        vec!["RealmName".to_string()]
    }
}

#[cfg(feature = "server")]
impl Header for RealmName {
    fn name() -> &'static HeaderName {
        static NAME: HeaderName = HeaderName::from_static("x-realm");
        &NAME
    }

    fn decode<'i, I>(values: &mut I) -> Result<Self, headers::Error>
    where
        I: Iterator<Item = &'i HeaderValue>,
    {
        Ok(values
            .next()
            .ok_or_else(headers::Error::invalid)?
            .to_str()
            .map_err(|_| headers::Error::invalid())?
            .parse()
            .map_err(|_| headers::Error::invalid())?)
    }

    fn encode<E>(&self, values: &mut E)
    where
        E: Extend<HeaderValue>,
    {
        values.extend(std::iter::once(
            HeaderValue::from_str(&self.to_string()).expect("Realm names only allow ascii 32-127"),
        ));
    }
}

#[cfg(test)]
mod test_realm_name {
    use super::*;

    #[test]
    fn test_valid() {
        assert!("test".parse::<RealmName>().is_ok());
        assert!("1default".parse::<RealmName>().is_ok());
        assert!("default".parse::<RealmName>().is_ok());
        assert!("default99".parse::<RealmName>().is_ok());
    }

    #[test]
    fn test_invalid() {
        assert!("t".parse::<RealmName>().is_err());
        assert!("".parse::<RealmName>().is_err());
        assert!("test*".parse::<RealmName>().is_err());
        assert!(
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                .parse::<RealmName>()
                .is_err()
        );
    }
}

#[data]
#[derive(Default)]
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

        #[cfg(feature = "server")]
        {
            for realm in realms.iter() {
                let realm_db = database.realm(realm.read().name.clone())?;

                let rw = realm_db.rw_transaction()?;
                let mut cluster_certs: Vec<RealmClusterCert> =
                    rw.scan().primary()?.all()?.collect::<Result<Vec<_>, _>>()?;

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
                        agent_cert.write("/tmp/agent.pem")?;
                        info!("Wrote agent cert to: /tmp/agent.pem");
                    }
                }

                // Get or create server cert
                let mut server_certs: Vec<RealmServerCert> =
                    rw.scan().primary()?.all()?.collect::<Result<Vec<_>, _>>()?;

                if server_certs.len() == 0 {
                    server_certs.push(cluster_certs[0].server_cert(instance.instance_id)?);
                    rw.insert(server_certs[0].clone())?;
                }

                rw.commit()?;
            }
        }

        // Import configured certs if newer than what we have in the database
        #[cfg(feature = "agent")]
        for path in config.agent_certs.as_ref().unwrap_or(&Vec::new()) {
            let new_cert = RealmAgentCert::read(path)?;

            let db = database.realm(new_cert.name()?)?;
            let rw = db.rw_transaction()?;
            let certs: Vec<RealmAgentCert> =
                rw.scan().primary()?.all()?.collect::<Result<Vec<_>, _>>()?;

            // Only import if the given certificate is newer than the one
            // already in the database.
            if let Some(old_cert) = certs.iter().find(|c| c.name().ok() == new_cert.name().ok()) {
                if new_cert.creation_time()? > old_cert.creation_time()? {
                    info!(path = %path.display(), "Importing updated realm certificate");
                    rw.upsert(new_cert)?;
                    rw.commit()?;
                }
            } else {
                info!(path = %path.display(), "Importing new realm certificate");
                rw.insert(new_cert)?;
                rw.commit()?;
            }
        }

        #[cfg(feature = "client")]
        for path in config.client_certs.as_ref().unwrap_or(&Vec::new()) {
            let new_cert = RealmClientCert::read(path)?;

            let db = database.realm(new_cert.name()?)?;
            let rw = db.rw_transaction()?;
            let certs: Vec<RealmClientCert> =
                rw.scan().primary()?.all()?.collect::<Result<Vec<_>, _>>()?;

            // Only import if the given certificate is newer than the one
            // already in the database.
            if let Some(old_cert) = certs.iter().find(|c| c.name().ok() == new_cert.name().ok()) {
                if new_cert.creation_time()? > old_cert.creation_time()? {
                    info!(path = %path.display(), "Importing updated realm certificate");
                    rw.upsert(new_cert)?;
                    rw.commit()?;
                }
            } else {
                info!(path = %path.display(), "Importing new realm certificate");
                rw.insert(new_cert)?;
                rw.commit()?;
            }
        }

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
                return self.database.realm(name);
            }
        }
        bail!("Realm does not exist");
    }

    #[cfg(feature = "client")]
    pub fn find_client_cert(&self, realm: RealmName) -> Result<RealmClientCert> {
        let db = self.realm(realm.clone())?;
        let r = db.r_transaction()?;

        {
            let certs: Vec<RealmClientCert> =
                r.scan().primary()?.all()?.collect::<Result<Vec<_>, _>>()?;

            for cert in certs {
                if cert.name()? == realm {
                    return Ok(cert);
                }
            }

            bail!("Failed to find cert");
        }
    }

    #[cfg(feature = "agent")]
    pub fn find_agent_cert(&self, realm: RealmName) -> Result<RealmAgentCert> {
        let db = self.realm(realm.clone())?;
        let r = db.r_transaction()?;

        {
            let certs: Vec<RealmAgentCert> =
                r.scan().primary()?.all()?.collect::<Result<Vec<_>, _>>()?;

            for cert in certs {
                if cert.name()? == realm {
                    return Ok(cert);
                }
            }

            bail!("Failed to find cert");
        }
    }
}

/// A realm is a set of clients and agents that can interact. Each realm has a
/// global CA certificate that signs certificates used to connect to the server.
///
/// All servers have a default realm called "default". All `RealmData` entries
/// are stored within this realm.
#[data]
#[derive(Default, Validate)]
pub struct RealmData {
    #[secondary_key(unique)]
    pub name: RealmName,
    pub owner: String,
}

/// The realm's global CA certificate.
#[data]
#[derive(Default)]
pub struct RealmClusterCert {
    pub name: RealmName,
    pub cert: Vec<u8>,
    pub key: Option<Vec<u8>>,
}

impl RealmClusterCert {
    pub fn cluster_id(&self) -> Result<ClusterId> {
        for ext in X509Certificate::from_der(&self.cert)?.1.iter_extensions() {
            if let ParsedExtension::SubjectAlternativeName(san) = ext.parsed_extension() {
                for name in &san.general_names {
                    if let GeneralName::DNSName(s) = name {
                        return s.parse::<ClusterId>();
                    }
                }
            }
        }

        bail!("Subject name not found");
    }
}

/// Each server in the cluster gets its own server certificate.
#[data(instance)]
#[derive(Default)]
pub struct RealmServerCert {
    pub cert: Vec<u8>,
    pub key: Option<Vec<u8>>,
}

impl RealmServerCert {
    pub fn subject_name(&self) -> Result<String> {
        for ext in X509Certificate::from_der(&self.cert)?.1.iter_extensions() {
            if let ParsedExtension::SubjectAlternativeName(san) = ext.parsed_extension() {
                for name in &san.general_names {
                    if let GeneralName::DNSName(s) = name {
                        return Ok(s.to_string());
                    }
                }
            }
        }

        bail!("Subject name not found");
    }
}

/// Realm certificate for client instances that can authenticate with a server
/// instance against a particular realm.
#[data]
#[derive(Default)]
pub struct RealmClientCert {
    pub ca: Vec<u8>,
    pub cert: Vec<u8>,
    pub key: Option<Vec<u8>>,
}

impl Validate for RealmClientCert {
    fn validate(&self) -> Result<(), ValidationErrors> {
        let mut errors = ValidationErrors::new();

        // Parse the certificate
        let cert = match X509Certificate::from_der(&self.cert) {
            Ok((_, cert)) => cert,
            Err(_) => {
                errors.add(
                    "cert",
                    ValidationError::new("Invalid X.509 certificate format"),
                );
                return Err(errors);
            }
        };

        // Validate extended key usage for clientAuth
        let mut client_auth = false;
        let mut client_realm = false;
        for ext in cert.iter_extensions() {
            if let ParsedExtension::ExtendedKeyUsage(eku) = ext.parsed_extension() {
                if eku.client_auth {
                    client_auth = true;
                }
                if eku
                    .other
                    .contains(&Oid::from(&[1, 1, 1, InstanceType::Client.mask() as u64]).unwrap())
                {
                    client_realm = true;
                }
            }
        }

        if !client_realm {
            errors.add(
                "cert",
                ValidationError::new("Certificate must have client extended key usage"),
            );
        }
        if !client_auth {
            errors.add(
                "cert",
                ValidationError::new("Certificate must have clientAuth extended key usage"),
            );
        }

        if errors.is_empty() {
            Ok(())
        } else {
            Err(errors)
        }
    }
}

impl RealmClientCert {
    pub fn cluster_id(&self) -> Result<ClusterId> {
        for ext in X509Certificate::from_der(&self.ca)?.1.iter_extensions() {
            if let ParsedExtension::SubjectAlternativeName(san) = ext.parsed_extension() {
                for name in &san.general_names {
                    if let GeneralName::DNSName(s) = name {
                        return s.parse::<ClusterId>();
                    }
                }
            }
        }

        bail!("Subject name not found");
    }

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

        cert.validate()?;
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

    #[cfg(feature = "client")]
    pub fn ca(&self) -> Result<reqwest::Certificate> {
        Ok(reqwest::Certificate::from_der(&self.ca)?)
    }

    #[cfg(feature = "client")]
    pub fn identity(&self) -> Result<reqwest::Identity> {
        // Combine cert and key together
        let mut bundle = Vec::new();
        bundle.extend_from_slice(encode(&Pem::new("CERTIFICATE", self.cert.clone())).as_bytes());
        bundle.extend_from_slice(
            encode(&Pem::new(
                "PRIVATE KEY",
                self.key.as_ref().ok_or_else(|| anyhow!("No key"))?.clone(),
            ))
            .as_bytes(),
        );
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
    #[cfg(feature = "server")]
    fn test_read_write() -> Result<()> {
        let cluster_id = sandpolis_instance::ClusterId::default();
        let ca = RealmClusterCert::new(cluster_id, "default".parse()?)?;
        let original_cert = ca.client_cert()?;

        let temp_file = tempfile::NamedTempFile::new()?;
        original_cert.write(temp_file.path())?;

        let read_cert = RealmClientCert::read(temp_file.path())?;

        assert_eq!(original_cert.ca, read_cert.ca);
        assert_eq!(original_cert.cert, read_cert.cert);
        assert_eq!(original_cert.key, read_cert.key);
        assert_eq!(original_cert.cluster_id()?, cluster_id);
        assert_eq!(read_cert.cluster_id()?, cluster_id);
        Ok(())
    }
}

#[cfg(test)]
mod test_agent_cert {
    use super::*;

    #[test]
    #[cfg(feature = "server")]
    fn test_read_write() -> Result<()> {
        let cluster_id = sandpolis_instance::ClusterId::default();
        let ca = RealmClusterCert::new(cluster_id, "default".parse()?)?;
        let original_cert = ca.agent_cert()?;

        let temp_file = tempfile::NamedTempFile::new()?;
        original_cert.write(temp_file.path())?;

        let read_cert = RealmAgentCert::read(temp_file.path())?;

        assert_eq!(original_cert.ca, read_cert.ca);
        assert_eq!(original_cert.cert, read_cert.cert);
        assert_eq!(original_cert.key, read_cert.key);
        assert_eq!(original_cert.cluster_id()?, cluster_id);
        assert_eq!(read_cert.cluster_id()?, cluster_id);
        Ok(())
    }
}

/// Realm certificate for agent instances that can authenticate with a server
/// instance against a particular realm.
#[data]
#[derive(Default)]
pub struct RealmAgentCert {
    pub ca: Vec<u8>,
    pub cert: Vec<u8>,
    pub key: Option<Vec<u8>>,
}

impl Validate for RealmAgentCert {
    fn validate(&self) -> Result<(), ValidationErrors> {
        let mut errors = ValidationErrors::new();

        // TODO check .name()

        // Parse the certificate
        let cert = match X509Certificate::from_der(&self.cert) {
            Ok((_, cert)) => cert,
            Err(_) => {
                errors.add(
                    "cert",
                    ValidationError::new("Invalid X.509 certificate format"),
                );
                return Err(errors);
            }
        };

        // Validate extended key usage for clientAuth
        let mut client_auth = false;
        let mut agent_realm = false;
        for ext in cert.iter_extensions() {
            if let ParsedExtension::ExtendedKeyUsage(eku) = ext.parsed_extension() {
                if eku.client_auth {
                    client_auth = true;
                }
                if eku
                    .other
                    .contains(&Oid::from(&[1, 1, 1, InstanceType::Agent.mask() as u64]).unwrap())
                {
                    agent_realm = true;
                }
            }
        }

        if !agent_realm {
            errors.add(
                "cert",
                ValidationError::new("Certificate must have agent extended key usage"),
            );
        }
        if !client_auth {
            errors.add(
                "cert",
                ValidationError::new("Certificate must have clientAuth extended key usage"),
            );
        }

        if errors.is_empty() {
            Ok(())
        } else {
            Err(errors)
        }
    }
}

impl RealmAgentCert {
    pub fn cluster_id(&self) -> Result<ClusterId> {
        for ext in X509Certificate::from_der(&self.ca)?.1.iter_extensions() {
            if let ParsedExtension::SubjectAlternativeName(san) = ext.parsed_extension() {
                for name in &san.general_names {
                    if let GeneralName::DNSName(s) = name {
                        return s.parse::<ClusterId>();
                    }
                }
            }
        }

        bail!("Subject name not found");
    }

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
                "AGENT CERTIFICATE" => {
                    cert.cert = item.into_contents();
                }
                "AGENT KEY" => {
                    cert.key = Some(item.into_contents());
                }
                _ => bail!("Invalid realm certificate"),
            }
        }

        assert!(!cert.ca.is_empty());
        assert!(!cert.cert.is_empty());

        cert.validate()?;
        Ok(cert)
    }

    /// Write the certificate to a file.
    pub fn write<P>(&self, path: P) -> Result<()>
    where
        P: AsRef<Path>,
    {
        let mut file = File::create(path)?;

        file.write_all(encode(&Pem::new("CLUSTER CERTIFICATE", self.ca.clone())).as_bytes())?;
        file.write_all(encode(&Pem::new("AGENT CERTIFICATE", self.cert.clone())).as_bytes())?;

        if let Some(key) = self.key.clone() {
            file.write_all(encode(&Pem::new("AGENT KEY", key)).as_bytes())?;
        }
        Ok(())
    }

    #[cfg(feature = "agent")]
    pub fn ca(&self) -> Result<reqwest::Certificate> {
        Ok(reqwest::Certificate::from_der(&self.ca)?)
    }

    #[cfg(feature = "agent")]
    pub fn identity(&self) -> Result<reqwest::Identity> {
        // Combine cert and key together
        let mut bundle = Vec::new();
        bundle.extend_from_slice(encode(&Pem::new("CERTIFICATE", self.cert.clone())).as_bytes());
        bundle.extend_from_slice(
            encode(&Pem::new(
                "PRIVATE KEY",
                self.key.as_ref().ok_or_else(|| anyhow!("No key"))?.clone(),
            ))
            .as_bytes(),
        );
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

pub enum RealmPermission {
    /// Right to create new realms on the server
    Create,
    /// Right to view all realms on the server
    List,
    /// Right to delete any realm
    Delete,
}
