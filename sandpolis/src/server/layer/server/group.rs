use crate::core::database::Collection;
use crate::core::database::Document;
use crate::core::layer::server::group::GroupCaCertificate;
use crate::core::layer::server::group::GroupClientCert;
use crate::core::layer::server::group::GroupData;
use crate::core::InstanceId;
use anyhow::Result;
use axum::{
    extract::Request, middleware::AddExtension, middleware::Next, response::Response, Extension,
};
use axum_server::tls_rustls::RustlsConfig;
use axum_server::{accept::Accept, tls_rustls::RustlsAcceptor};
use futures_util::future::BoxFuture;
use rcgen::BasicConstraints;
use rcgen::Certificate;
use rcgen::CertificateParams;
use rcgen::DistinguishedName;
use rcgen::DnType;
use rcgen::ExtendedKeyUsagePurpose;
use rcgen::IsCa;
use rcgen::KeyPair;
use rcgen::SanType;
use rustls::server::ResolvesServerCertUsingSni;
use rustls::server::WebPkiClientVerifier;
use rustls::RootCertStore;
use rustls::ServerConfig;
use rustls_pki_types::pem::PemObject;
use rustls_pki_types::CertificateDer;
use rustls_pki_types::PrivateKeyDer;
use serde::{Deserialize, Serialize};
use std::io;
use std::sync::Arc;
use time::OffsetDateTime;
use tokio::io::{AsyncRead, AsyncWrite};
use tokio_rustls::server::TlsStream;
use tower::Layer;
use tracing::debug;
use validator::Validate;
use x509_parser::prelude::{FromDer, X509Certificate};

impl GroupCaCertificate {
    /// Generate a new group CA certificate.
    pub fn new(server_id: InstanceId) -> Result<Self> {
        // Generate key
        let keypair = KeyPair::generate()?;

        // Generate certificate
        let mut cert_params = CertificateParams::default();
        cert_params.is_ca = IsCa::Ca(BasicConstraints::Unconstrained);
        cert_params.not_before = OffsetDateTime::now_utc();
        cert_params.subject_alt_names = vec![SanType::DnsName("test".try_into()?)];
        cert_params.distinguished_name = DistinguishedName::new();
        // cert_params
        //     .distinguished_name
        //     .push(DnType::OrganizationName, "s7s");
        cert_params
            .distinguished_name
            .push(DnType::CommonName, "test".to_string()); // server_id.to_string());

        // Generate the certificate
        let cert = cert_params.self_signed(&keypair)?;

        debug!(cert = ?cert.params(), "Generated new group CA certificate");
        Ok(Self {
            cert: cert.pem(),
            key: keypair.serialize_pem(),
        })
    }

    pub fn ca(&self) -> Result<Certificate> {
        // TODO https://github.com/rustls/rcgen/issues/274

        Ok(CertificateParams::from_ca_cert_der(
            &pem::parse(&self.cert)?.into_contents().try_into()?,
        )?
        .self_signed(&KeyPair::from_pem(&self.key)?)?)
    }

    /// Generate a new certificate signed by the group's CA.
    pub fn generate_cert(&self, group_name: &str) -> Result<GroupClientCert> {
        // Generate key
        let keypair = KeyPair::generate()?;

        // Generate certificate
        let mut cert_params = CertificateParams::default();
        cert_params
            .extended_key_usages
            .push(ExtendedKeyUsagePurpose::ClientAuth);
        cert_params.not_before = OffsetDateTime::now_utc();
        cert_params
            .distinguished_name
            .push(DnType::CommonName, group_name.to_string());
        // TODO not_after of 1 month

        // Generate the certificate signed by the CA
        let cert = cert_params.signed_by(&keypair, &self.ca()?, &KeyPair::from_pem(&self.key)?)?;

        Ok(GroupClientCert {
            cert: cert.pem(),
            key: keypair.serialize_pem(),
        })
    }
}

#[derive(Debug, Clone)]
pub struct GroupAuth(String);

#[derive(Debug, Clone)]
pub struct TlsData {
    peer_certificates: Option<Vec<CertificateDer<'static>>>,
}

#[derive(Debug, Clone)]
pub struct GroupAcceptor(RustlsAcceptor);

impl GroupAcceptor {
    pub fn new(groups: Collection<GroupData>) -> Result<Self> {
        // TODO add all groups to the cert store
        let mut roots = RootCertStore::empty();

        // TODO add server certs
        let mut sni_resolver = ResolvesServerCertUsingSni::new();

        let config = ServerConfig::builder();

        for group in groups.documents() {
            let group = group?;
            let ca: Document<GroupCaCertificate> = group.get_document("ca")?.unwrap();

            roots.add(pem::parse(&ca.data.cert)?.into_contents().try_into()?)?;

            let private_key = config
                .crypto_provider()
                .key_provider
                .load_private_key(PrivateKeyDer::from_pem_slice(&ca.data.key.as_bytes())?)?;

            sni_resolver.add(
                "test", //&group.data.name,
                rustls::sign::CertifiedKey::new(
                    vec![pem::parse(&ca.data.cert)?.into_contents().try_into()?],
                    private_key,
                ),
            )?;

            // TODO
            break;
        }

        Ok(Self(RustlsAcceptor::new(RustlsConfig::from_config(
            Arc::new(
                config
                    .with_client_cert_verifier(
                        WebPkiClientVerifier::builder(Arc::new(roots)).build()?,
                    )
                    .with_cert_resolver(Arc::new(sni_resolver)),
            ),
        ))))
    }
}
impl<I, S> Accept<I, S> for GroupAcceptor
where
    I: AsyncRead + AsyncWrite + Unpin + Send + 'static,
    S: Send + 'static,
{
    type Stream = TlsStream<I>;
    type Service = AddExtension<S, TlsData>;
    type Future = BoxFuture<'static, io::Result<(Self::Stream, Self::Service)>>;

    fn accept(&self, stream: I, service: S) -> Self::Future {
        let acceptor = self.0.clone();

        Box::pin(async move {
            let (stream, service) = acceptor.accept(stream, service).await?;
            let server_conn = stream.get_ref().1;
            let tls_data = TlsData {
                peer_certificates: server_conn.peer_certificates().map(From::from),
            };
            let service = Extension(tls_data).layer(service);

            Ok((stream, service))
        })
    }
}

pub async fn auth_middleware(
    Extension(tls_data): Extension<TlsData>,
    mut request: Request,
    next: Next,
) -> Result<Response, &'static str> {
    if let Some(peer_certificates) = tls_data.peer_certificates {
        // Take first client certificate
        let cert = X509Certificate::from_der(
            &peer_certificates
                .first()
                .ok_or("missing client certificate")?,
        )
        .map_err(|_| "invalid client certificate")?
        .1;

        // Take first common name from certificate
        let cn = cert
            .subject()
            .iter_common_name()
            .next()
            .ok_or("missing common name in client certificate")?
            .as_str()
            .map_err(|_| "invalid common name in client certificate")?;

        // Pass authentication to routes
        request.extensions_mut().insert(GroupAuth(cn.to_string()));
    } else {
        return Err("missing client certificate");
    }

    Ok(next.run(request).await)
}
