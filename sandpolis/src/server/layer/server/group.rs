use crate::core::database::Collection;
use crate::core::layer::server::group::GroupCaCertificate;
use crate::core::layer::server::group::GroupCertificate;
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
use rustls::server::WebPkiClientVerifier;
use rustls::Certificate;
use rustls::RootCertStore;
use rustls::ServerConfig;
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

        // Set common name to the group's name
        cert_params.distinguished_name = DistinguishedName::new();
        cert_params
            .distinguished_name
            .push(DnType::OrganizationName, "s7s");
        cert_params
            .distinguished_name
            .push(DnType::CommonName, server_id.to_string());

        // Generate the certificate
        let cert = cert_params.self_signed(&keypair)?;

        debug!(cert = ?cert.params(), "Generated new group CA certificate");
        Ok(Self {
            cert: cert.pem(),
            key: keypair.serialize_pem(),
        })
    }

    /// Generate a new certificate signed by the group's CA.
    pub fn generate_cert(&self) -> Result<GroupCertificate> {
        // Generate key
        let keypair = KeyPair::generate()?;

        // Generate certificate
        let mut cert_params = CertificateParams::default();
        cert_params
            .extended_key_usages
            .push(ExtendedKeyUsagePurpose::ClientAuth);
        cert_params.not_before = OffsetDateTime::now_utc();
        // TODO not_after of 1 month

        // Generate the certificate
        let issuer_der = pem::parse(&self.cert)?;
        let cert = cert_params.signed_by(&keypair, todo!(), &KeyPair::from_pem(&self.key)?)?;

        Ok(GroupCertificate {
            cert: cert.pem(),
            key: keypair.serialize_pem(),
        })
    }
}

#[derive(Debug, Clone)]
pub struct GroupAuth {}

#[derive(Debug, Clone)]
pub struct TlsData {
    peer_certificates: Option<Vec<rustls::Certificate>>,
}

#[derive(Debug, Clone)]
pub struct GroupAcceptor(RustlsAcceptor);

impl GroupAcceptor {
    pub fn new(groups: Collection<GroupData>) -> Result<Self> {
        let roots = RootCertStore::empty();

        // TODO add all groups
        let verifier = WebPkiClientVerifier::builder(Arc::new(roots));
        Ok(Self(RustlsAcceptor::new(RustlsConfig::from_config(
            Arc::new(
                ServerConfig::builder()
                    .with_client_cert_verifier(Arc::new(verifier.build()?))
                    .with_single_cert(certs, private_key)
                    .unwrap(),
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
                .ok_or("missing client certificate")?
                .0,
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
        request.extensions_mut().insert(GroupAuth {});
    } else {
        return Err("missing client certificate");
    }

    Ok(next.run(request).await)
}
