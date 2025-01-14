use crate::core::layer::server::group::GroupData;
use anyhow::Result;
use rcgen::Certificate;
use rcgen::CertificateParams;
use rcgen::DistinguishedName;
use rcgen::DnType;
use rcgen::KeyPair;
use serde::{Deserialize, Serialize};
use time::OffsetDateTime;
use validator::Validate;

impl GroupData {
    /// Create a new group. This will generate a new group CA certificate.
    pub fn new(name: &str) -> Result<Self> {
        // Generate key
        let keypair = KeyPair::generate()?;

        // Generate certificate
        let mut cert_params = CertificateParams::default();
        cert_params.not_before = OffsetDateTime::now_utc();

        // Set common name to the group's name
        cert_params.distinguished_name = DistinguishedName::new();
        cert_params
            .distinguished_name
            .push(DnType::OrganizationName, "s7s");
        cert_params
            .distinguished_name
            .push(DnType::CommonName, name);

        // Generate the certificate
        let cert = cert_params.self_signed(&keypair)?;

        Ok(GroupData {
            id: todo!(),
            name: name.to_string(),
            members: Vec::new(),
            ca: cert.pem(),
            key: keypair.serialize_pem(),
        })
    }

    /// Produce a new certificate signed by the group's CA.
    pub fn generate_cert(&self) {}
}
