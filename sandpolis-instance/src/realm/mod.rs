use crate::ClusterId;
use crate::InstanceLayer;
use crate::InstanceType;
use crate::database::RealmDatabase;
use crate::database::ResidentVec;
use crate::database::{DatabaseLayer, Resident};
use anyhow::Result;
use anyhow::anyhow;
use anyhow::bail;
use config::RealmConfig;
use native_db::ToKey;
use native_model::Model;
use pem::Pem;
use pem::encode;
use regex::Regex;
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

#[cfg(not(target_os = "android"))]
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

    /// Agent realm certs loaded from `--realm-cert`. Kept in memory only.
    #[cfg(feature = "agent")]
    agent_certs: Vec<RealmAgentCert>,

    /// Client realm certs loaded from `--realm-cert`. Kept in memory only.
    ///
    /// Also used by a local stratum server to authenticate to its global stratum
    /// server — there is no separate server-to-server certificate type.
    #[cfg(any(feature = "client", feature = "server"))]
    client_certs: Vec<RealmClientCert>,
}

impl RealmLayer {
    /// `authoritative` is true only on the global stratum server, which owns the
    /// realm CA.
    ///
    /// A local stratum server must never mint a CA of its own — that would make
    /// it a separate trust root and its agents unreachable from the rest of the
    /// network. It starts with no certificates and is issued a server
    /// certificate by the global stratum server (see
    /// [`install_enrollment`](Self::install_enrollment)).
    pub async fn new(
        config: RealmConfig,
        database: DatabaseLayer,
        instance: InstanceLayer,
        #[allow(unused_variables)] authoritative: bool,
    ) -> Result<Self> {
        debug!("Initializing realm layer");

        let default_realm = database.realm(RealmName::default())?;

        // These records have to be stored in the default realm so we know what
        // other realms exist.
        let realms: ResidentVec<RealmData> = default_realm.resident_vec(())?;

        if realms.len() == 0 {
            // Realm membership isn't replicated, so even a read-only replica has
            // to record the default realm for itself.
            realms.push_local(RealmData::default())?;
        }

        #[cfg(feature = "agent")]
        let mut agent_certs = Vec::new();
        #[cfg(any(feature = "client", feature = "server"))]
        #[allow(unused_mut)]
        let mut client_certs = Vec::new();

        // Only the global stratum server holds the realm CA and can issue from
        // it. A local stratum server gets its server certificate from the GS
        // instead, so that the whole network shares one trust root.
        #[cfg(feature = "server")]
        if authoritative {
            for realm in realms.iter() {
                let realm_db = database.realm(realm.read().name.clone())?;

                // This instance's own certificates are local state, not estate
                // data replicated from the global stratum server.
                let rw = realm_db.local_write()?;
                let mut cluster_certs: Vec<RealmClusterCert> =
                    rw.scan().primary()?.all()?.collect::<Result<Vec<_>, _>>()?;

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

                // When the client and/or agent are compiled into the same
                // binary (the "all-in-one" build), derive their realm certs from
                // the local cluster CA and keep them in memory. This lets a
                // co-located client/agent connect to the local server over
                // loopback without an out-of-band `--realm-cert`.
                //
                // Only possible here, where the CA is: an all-in-one local
                // stratum server needs `--realm-cert` for its co-located client.
                #[cfg(feature = "client")]
                client_certs.push(cluster_certs[0].client_cert()?);
                #[cfg(feature = "agent")]
                agent_certs.push(cluster_certs[0].agent_cert()?);

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

        // Load realm certs from the paths supplied on the command line. These
        // are kept in memory only and used directly when connecting to a
        // server; nothing is persisted to the database, so they must be
        // supplied on every run.
        for path in &config.realm_certs {
            #[allow(unused_mut, unused_assignments)]
            let mut loaded = false;

            #[cfg(feature = "agent")]
            if let Ok(cert) = RealmAgentCert::read(path) {
                info!(path = %path.display(), "Loaded agent realm certificate");
                agent_certs.push(cert);
                loaded = true;
            }

            #[cfg(any(feature = "client", feature = "server"))]
            if !loaded {
                if let Ok(cert) = RealmClientCert::read(path) {
                    info!(path = %path.display(), "Loaded client realm certificate");
                    client_certs.push(cert);
                    loaded = true;
                }
            }

            if !loaded {
                bail!("Failed to load realm certificate: {}", path.display());
            }
        }

        Ok(Self {
            database,
            data: default_realm.resident(())?,
            realms,
            #[cfg(feature = "agent")]
            agent_certs,
            #[cfg(any(feature = "client", feature = "server"))]
            client_certs,
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

    /// Whether this instance already holds the certificates it needs to serve
    /// `realm`: the realm CA (to verify peers) and its own server certificate
    /// (to present).
    #[cfg(feature = "server")]
    pub fn has_server_cert(&self, realm: RealmName, instance_id: crate::InstanceId) -> bool {
        let Ok(db) = self.database.realm(realm) else {
            return false;
        };
        let Ok(r) = db.r_transaction() else {
            return false;
        };

        let has_ca = (|| -> Result<bool> {
            let cas: Vec<RealmClusterCert> =
                r.scan().primary()?.all()?.collect::<Result<Vec<_>, _>>()?;
            Ok(!cas.is_empty())
        })()
        .unwrap_or(false);

        let has_cert = (|| -> Result<bool> {
            let certs: Vec<RealmServerCert> =
                r.scan().primary()?.all()?.collect::<Result<Vec<_>, _>>()?;
            Ok(certs.iter().any(|c| c._instance_id == instance_id))
        })()
        .unwrap_or(false);

        has_ca && has_cert
    }

    /// Store the realm CA and server certificate issued by the global stratum
    /// server.
    ///
    /// `ca` is the CA certificate **without** its private key — a local stratum
    /// server can verify peers against the realm's trust root but can never
    /// issue from it. These are this instance's own credentials, so they are
    /// written locally even though the database is a replica.
    #[cfg(feature = "server")]
    pub fn install_enrollment(
        &self,
        realm: RealmName,
        ca: Vec<u8>,
        cert: Vec<u8>,
        key: Vec<u8>,
        instance_id: crate::InstanceId,
    ) -> Result<()> {
        let db = self.database.realm(realm.clone())?;
        let rw = db.local_write()?;

        let existing: Vec<RealmClusterCert> =
            rw.scan().primary()?.all()?.collect::<Result<Vec<_>, _>>()?;
        for old in existing {
            rw.remove(old)?;
        }
        rw.insert(RealmClusterCert {
            name: realm.clone(),
            cert: ca,
            key: None,
            ..Default::default()
        })?;

        let existing: Vec<RealmServerCert> =
            rw.scan().primary()?.all()?.collect::<Result<Vec<_>, _>>()?;
        for old in existing {
            if old._instance_id == instance_id {
                rw.remove(old)?;
            }
        }
        rw.insert(RealmServerCert {
            cert,
            key: Some(key),
            _instance_id: instance_id,
            ..Default::default()
        })?;

        rw.commit()?;
        info!(realm = %realm, "Installed server certificate issued by the global stratum server");
        Ok(())
    }

    #[cfg(any(feature = "client", feature = "server"))]
    pub fn find_client_cert(&self, realm: RealmName) -> Result<RealmClientCert> {
        for cert in &self.client_certs {
            if cert.name()? == realm {
                return Ok(cert.clone());
            }
        }

        bail!("No client realm certificate loaded for realm: {realm}");
    }

    #[cfg(feature = "agent")]
    pub fn find_agent_cert(&self, realm: RealmName) -> Result<RealmAgentCert> {
        for cert in &self.agent_certs {
            if cert.name()? == realm {
                return Ok(cert.clone());
            }
        }

        bail!("No agent realm certificate loaded for realm: {realm}");
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

    #[cfg(any(feature = "client", feature = "server"))]
    pub fn ca(&self) -> Result<reqwest::Certificate> {
        Ok(reqwest::Certificate::from_der(&self.ca)?)
    }

    #[cfg(any(feature = "client", feature = "server"))]
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

#[cfg(all(test, feature = "server"))]
mod test_enrollment {
    use super::*;
    use crate::database::DatabaseAccess;
    use crate::{InstanceId, InstanceType};

    fn models() -> &'static native_db::Models {
        static MODELS: std::sync::OnceLock<native_db::Models> = std::sync::OnceLock::new();
        MODELS.get_or_init(|| {
            let mut m = native_db::Models::new();
            m.define::<RealmLayerData>().unwrap();
            m.define::<RealmData>().unwrap();
            m.define::<RealmClusterCert>().unwrap();
            m.define::<RealmServerCert>().unwrap();
            m
        })
    }

    fn replica() -> Result<DatabaseLayer> {
        Ok(DatabaseLayer::new(
            crate::database::config::DatabaseConfig {
                storage: None,
                ephemeral: true,
                key: Default::default(),
            },
            models(),
            DatabaseAccess::Replica,
        )?)
    }

    fn layer(database: DatabaseLayer) -> RealmLayer {
        RealmLayer {
            data: database
                .realm(RealmName::default())
                .unwrap()
                .resident(())
                .unwrap(),
            realms: database
                .realm(RealmName::default())
                .unwrap()
                .resident_vec(())
                .unwrap(),
            database,
            #[cfg(feature = "agent")]
            agent_certs: Vec::new(),
            #[cfg(any(feature = "client", feature = "server"))]
            client_certs: Vec::new(),
        }
    }

    /// A local stratum server starts with nothing: it must not invent a CA, and
    /// it can't serve until the global stratum server has issued its cert.
    #[tokio::test]
    async fn replica_starts_without_certificates() -> Result<()> {
        let realms = layer(replica()?);
        let id = InstanceId::new(&[InstanceType::Server]);
        assert!(!realms.has_server_cert(RealmName::default(), id));
        Ok(())
    }

    /// Installing what the global stratum server issued makes the server ready,
    /// and the CA arrives without its private key so a local stratum server can
    /// verify peers but never issue certificates of its own.
    #[tokio::test]
    async fn enrollment_installs_ca_without_its_key() -> Result<()> {
        let cluster_id = crate::ClusterId::default();
        let ca = RealmClusterCert::new(cluster_id, RealmName::default())?;
        let id = InstanceId::new(&[InstanceType::Server]);
        let issued = ca.server_cert(id)?;

        let realms = layer(replica()?);
        realms.install_enrollment(
            RealmName::default(),
            ca.cert.clone(),
            issued.cert.clone(),
            issued.key.clone().expect("issued cert carries a key"),
            id,
        )?;

        assert!(realms.has_server_cert(RealmName::default(), id));

        let db = realms.database.realm(RealmName::default())?;
        let r = db.r_transaction()?;
        let stored: Vec<RealmClusterCert> =
            r.scan().primary()?.all()?.collect::<Result<Vec<_>, _>>()?;

        assert_eq!(stored.len(), 1, "exactly one CA is stored");
        assert_eq!(stored[0].cert, ca.cert, "the CA certificate is the GS's");
        assert!(
            stored[0].key.is_none(),
            "the CA private key must never reach a local stratum server"
        );
        assert!(
            stored[0].ca().is_err(),
            "without the key, a local stratum server cannot issue certificates"
        );
        Ok(())
    }

    /// Re-enrolling replaces the previous credentials rather than accumulating
    /// them, so `resident()` (which expects a singleton CA) keeps working.
    #[tokio::test]
    async fn re_enrolling_replaces_credentials() -> Result<()> {
        let id = InstanceId::new(&[InstanceType::Server]);
        let realms = layer(replica()?);

        for _ in 0..2 {
            let ca = RealmClusterCert::new(crate::ClusterId::default(), RealmName::default())?;
            let issued = ca.server_cert(id)?;
            realms.install_enrollment(
                RealmName::default(),
                ca.cert.clone(),
                issued.cert,
                issued.key.expect("issued cert carries a key"),
                id,
            )?;
        }

        let db = realms.database.realm(RealmName::default())?;
        let r = db.r_transaction()?;
        let cas: Vec<RealmClusterCert> =
            r.scan().primary()?.all()?.collect::<Result<Vec<_>, _>>()?;
        let certs: Vec<RealmServerCert> =
            r.scan().primary()?.all()?.collect::<Result<Vec<_>, _>>()?;

        assert_eq!(cas.len(), 1);
        assert_eq!(certs.len(), 1);
        Ok(())
    }

    /// A server certificate is only ever issued to a server.
    #[tokio::test]
    async fn server_cert_requires_a_server_id() -> Result<()> {
        let ca = RealmClusterCert::new(crate::ClusterId::default(), RealmName::default())?;
        assert!(
            ca.server_cert(InstanceId::new(&[InstanceType::Agent]))
                .is_err()
        );
        Ok(())
    }
}

#[cfg(test)]
mod test_client_cert {
    use super::*;

    #[test]
    #[cfg(feature = "server")]
    fn test_read_write() -> Result<()> {
        let cluster_id = crate::ClusterId::default();
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
        let cluster_id = crate::ClusterId::default();
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
