use anyhow::Result;
use rcgen::Certificate;
use rcgen::CertificateParams;
use rcgen::DistinguishedName;
use rcgen::DnType;
use rsa::pkcs8::ToPrivateKey;
use rsa::RsaPrivateKey;
use serde::{Deserialize, Serialize};
use time::OffsetDateTime;
use validator::Validate;

#[derive(Clone, Serialize, Deserialize, Validate)]
pub struct Group {
    #[validate(length(min = 4), length(max = 20))]
    pub name: String,

    pub ca: Vec<GroupCA>,
}

#[derive(Clone, Serialize, Deserialize, Validate)]
struct GroupCA {
    pub cert_pem: String,
    pub key_pem: String,
}

impl Group {
    pub fn new(name: &str) -> Result<Self> {
        let mut ca_params = CertificateParams::default();
        ca_params.alg = &rcgen::PKCS_RSA_SHA256;
        ca_params.not_before = OffsetDateTime::now_utc();

        // Set common name to the group's name
        ca_params.distinguished_name = DistinguishedName::new();
        ca_params
            .distinguished_name
            .push(DnType::OrganizationName, "s7s");
        ca_params.distinguished_name.push(DnType::CommonName, name);

        // Generate private key
        let private_key = RsaPrivateKey::new()?.to_pkcs8_der()?;
        ca_params.key_pair = Some(key_pair);

        // Generate the certificate
        let cert = Certificate::from_params(ca_params)?;

        Ok(Group {
            name: name.to_string(),
            ca: vec![GroupCA {
                cert_pem: cert.serialize_pem()?,
                key_pem: cert.serialize_private_key_pem(),
            }],
        })
    }

    /// Produce a new certificate signed by the group's CA. If the group has more than one CA defined, the most recent is used.
    pub fn generate_cert(&self) {}
}
