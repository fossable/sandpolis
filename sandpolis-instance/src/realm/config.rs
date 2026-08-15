use crate::realm::RealmName;
use crate::realm::url::ServerUrl;
use anyhow::{Context, Result, bail};
use pem::{Pem, encode};
use serde::{Deserialize, Serialize};
use std::path::{Path, PathBuf};

/// PEM tag for an X.509 certificate.
pub const CERTIFICATE_TAG: &str = "CERTIFICATE";

/// PEM tag for a PKCS#8 private key.
pub const PRIVATE_KEY_TAG: &str = "PRIVATE KEY";

/// A certificate or private key as it appears in a `.realm` or `.server` file.
///
/// Both forms hold PEM: either written out in the file itself, or in a separate
/// file next to it. Inline is what the server writes back when it generates a
/// realm CA; a path is convenient when the material is managed elsewhere (a
/// secret manager, an ACME client, etc).
#[derive(Serialize, Deserialize, Debug, Clone, PartialEq, Eq)]
pub enum CertSource {
    Inline(String),
    Path(PathBuf),
}

impl CertSource {
    /// Wrap DER bytes as inline PEM with `tag`.
    pub fn inline_der(der: &[u8], tag: &str) -> Self {
        Self::Inline(encode(&Pem::new(tag, der.to_vec())))
    }

    /// Decode this source into DER bytes, requiring the PEM block to carry
    /// `tag`.
    ///
    /// A relative [`Path`](Self::Path) resolves against `base_dir`, which is the
    /// directory holding the file this source was read from.
    pub fn load_der(&self, base_dir: Option<&Path>, tag: &str) -> Result<Vec<u8>> {
        let pem = match self {
            Self::Inline(contents) => contents.clone(),
            Self::Path(path) => {
                let path = match base_dir {
                    Some(base) if path.is_relative() => base.join(path),
                    _ => path.clone(),
                };
                std::fs::read_to_string(&path)
                    .with_context(|| format!("Reading {}", path.display()))?
            }
        };

        let parsed = pem::parse(pem.as_bytes()).context("Parsing PEM")?;
        if parsed.tag() != tag {
            bail!("Expected a {tag} PEM block but found {}", parsed.tag());
        }
        Ok(parsed.into_contents())
    }
}

/// A realm's root certificate authority, as declared in a `.realm` file.
///
/// The global stratum server needs the key to issue certificates; a CA without
/// one can only verify.
#[derive(Serialize, Deserialize, Debug, Clone, PartialEq, Eq)]
pub struct CaConfig {
    pub cert: CertSource,
    pub key: Option<CertSource>,
}

impl CaConfig {
    /// Decode the CA certificate and (if present) its private key into DER.
    pub fn load_der(&self, base_dir: Option<&Path>) -> Result<(Vec<u8>, Option<Vec<u8>>)> {
        let cert = self.load_cert_der(base_dir)?;
        let key = match &self.key {
            Some(key) => Some(key.load_der(base_dir, PRIVATE_KEY_TAG)?),
            None => None,
        };
        Ok((cert, key))
    }

    /// Decode just the CA certificate into DER.
    fn load_cert_der(&self, base_dir: Option<&Path>) -> Result<Vec<u8>> {
        self.cert.load_der(base_dir, CERTIFICATE_TAG)
    }
}

/// Everything a server needs to bring one realm up: what it's called, the trust
/// root it serves under, and the address the certificates it mints will name.
///
/// A realm only ever comes from a `.realm` file (or the implicit default of a
/// zero-flag run), so this is the complete set of realms for a process.
#[derive(Debug, Clone, Default)]
pub struct RealmBootstrap {
    pub name: RealmName,

    /// The realm CA as `(certificate, key)` DER. Absent when the file declared
    /// no CA, in which case the server reuses the copy in the realm database or
    /// mints a fresh one and writes it back to the file.
    pub ca: Option<(Vec<u8>, Vec<u8>)>,

    /// Address clients and agents will use to reach this realm. Certificates
    /// minted for the realm carry it in their common name, so it must be how the
    /// server is actually reachable.
    pub address: Option<ServerUrl>,
}

/// Configures the agent's "polling" connection mode.
///
/// Lives with the realm file formats because it is carried in the `.server`
/// file that also holds the agent's certificate — an agent is handed its whole
/// connection policy in one file.
#[derive(Serialize, Deserialize, Debug, Clone, PartialEq, Eq)]
pub struct PollConfig {
    /// Cron expression describing when the agent connects to check in, e.g.
    /// `"0 */5 * * * *"` for every five minutes.
    pub schedule: String,

    /// How long the agent stays connected during each check-in window, in
    /// seconds. The server pulls the agent's accumulated data and delivers any
    /// pending work during this window before the connection is closed again.
    #[serde(default = "PollConfig::default_timeout_secs")]
    pub timeout_secs: u64,
}

impl PollConfig {
    pub const fn default_timeout_secs() -> u64 {
        30
    }
}

/// A `.server` file: everything an instance needs to trust and reach one
/// server, and nothing else.
///
/// The certificate's common name encodes the [`ServerUrl`], so the file names
/// the server it belongs to; there is no separate address field to keep in
/// sync. Whether the holder is a client or an agent follows from the
/// certificate's extended key usage.
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct ServerCertFile {
    /// The realm's cluster CA certificate, which verifies the server.
    pub ca: CertSource,

    /// This instance's client- or agent-type certificate. Its common name is
    /// the server's [`ServerUrl::canonical`] form.
    pub cert: CertSource,

    pub key: Option<CertSource>,

    /// Present to run an agent in polling mode instead of staying continuously
    /// connected.
    #[serde(default)]
    pub poll: Option<PollConfig>,
}

/// RON parsing options for the file formats: allow optional fields without an
/// explicit `Some`.
pub fn ron_options() -> ron::Options {
    ron::Options::default().with_default_extension(ron::extensions::Extensions::IMPLICIT_SOME)
}

impl ServerCertFile {
    /// Build the file contents for an endpoint certificate.
    pub fn from_endpoint(cert: &super::RealmCert, poll: Option<PollConfig>) -> Self {
        Self {
            ca: CertSource::inline_der(&cert.ca, CERTIFICATE_TAG),
            cert: CertSource::inline_der(&cert.cert, CERTIFICATE_TAG),
            key: cert
                .key
                .as_ref()
                .map(|key| CertSource::inline_der(key, PRIVATE_KEY_TAG)),
            poll,
        }
    }

    /// Serialize to the RON text that goes in a `.server` file.
    pub fn to_ron(&self) -> Result<String> {
        Ok(ron::ser::to_string_pretty(
            self,
            ron::ser::PrettyConfig::default(),
        )?)
    }

    pub fn write<P>(&self, path: P) -> Result<()>
    where
        P: AsRef<Path>,
    {
        std::fs::write(path, self.to_ron()?)?;
        Ok(())
    }

    /// Read a `.server` file and decode the certificate it holds.
    ///
    /// Relative paths inside the file resolve against the file's own directory.
    pub fn load<P>(path: P) -> Result<(super::RealmCert, Option<PollConfig>)>
    where
        P: AsRef<Path>,
    {
        let path = path.as_ref();
        let contents =
            std::fs::read_to_string(path).with_context(|| format!("Reading {}", path.display()))?;
        let file: Self = ron_options()
            .from_str(&contents)
            .with_context(|| format!("Parsing {}", path.display()))?;

        file.decode(path.parent())
    }

    /// Decode the certificate material into an endpoint certificate.
    ///
    /// The realm it authenticates against comes from the common name, so the
    /// file never has to name it separately.
    pub fn decode(&self, base_dir: Option<&Path>) -> Result<(super::RealmCert, Option<PollConfig>)> {
        use validator::Validate;

        let ca = self.ca.load_der(base_dir, CERTIFICATE_TAG)?;
        let cert = self.cert.load_der(base_dir, CERTIFICATE_TAG)?;
        let key = match &self.key {
            Some(key) => Some(key.load_der(base_dir, PRIVATE_KEY_TAG)?),
            None => None,
        };

        let mut endpoint = super::RealmCert {
            cert_type: super::RealmCertType::Endpoint,
            ca,
            cert,
            key,
            ..Default::default()
        };
        endpoint
            .validate()
            .context("Certificate is not an endpoint realm certificate")?;
        endpoint.name = endpoint.url()?.realm;

        Ok((endpoint, self.poll.clone()))
    }
}
