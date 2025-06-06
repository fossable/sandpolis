//! Server implementation

use super::GroupCaCert;
use super::GroupClientCert;
use super::GroupData;
use super::GroupName;
use super::GroupServerCert;
use anyhow::Result;
use anyhow::bail;
use axum::{
    Extension, extract::Request, middleware::AddExtension, middleware::Next, response::Response,
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
use rustls::RootCertStore;
use rustls::ServerConfig;
use rustls::server::ResolvesServerCertUsingSni;
use rustls::server::WebPkiClientVerifier;
use rustls_pki_types::CertificateDer;
use rustls_pki_types::PrivateKeyDer;
use rustls_pki_types::pem::PemObject;
use sandpolis_core::ClusterId;
use sandpolis_core::InstanceId;
use sandpolis_core::InstanceType;
use sandpolis_database::Collection;
use sandpolis_database::Document;
use std::io;
use std::sync::Arc;
use tempfile::TempDir;
use tempfile::tempdir;
use time::OffsetDateTime;
use tokio::io::{AsyncRead, AsyncWrite};
use tokio_rustls::server::TlsStream;
use tower::Layer;
use tracing::debug;
use x509_parser::prelude::{FromDer, X509Certificate};

impl super::GroupCaCert {
    /// Generate a new group CA certificate.
    pub fn new(cluster_id: ClusterId, name: GroupName) -> Result<Self> {
        // Generate key
        let keypair = KeyPair::generate()?;

        // Generate certificate
        let mut cert_params = CertificateParams::default();
        cert_params.is_ca = IsCa::Ca(BasicConstraints::Unconstrained);
        cert_params.not_before = OffsetDateTime::now_utc();
        cert_params.subject_alt_names = vec![SanType::DnsName(cluster_id.to_string().try_into()?)];

        // TODO still needed?
        cert_params.distinguished_name = DistinguishedName::new();
        cert_params
            .distinguished_name
            .push(DnType::CommonName, cluster_id.to_string());

        // Generate the certificate
        let cert = cert_params.self_signed(&keypair)?;

        debug!(cert = ?cert.params(), "Generated new group CA certificate");
        Ok(Self {
            name,
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

    /// Generate a new _clientAuth_ certificate signed by the group's CA.
    pub fn client_cert(&self) -> Result<GroupClientCert> {
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
            .push(DnType::CommonName, &*self.name);
        // TODO not_after of 1 month

        // Generate the certificate signed by the CA
        let cert = cert_params.signed_by(&keypair, &self.ca()?, &KeyPair::from_pem(&self.key)?)?;

        debug!(cert = ?cert.params(), "Generated new group client certificate");
        Ok(GroupClientCert {
            ca: self.ca()?.pem(),
            cert: cert.pem(),
            key: keypair.serialize_pem(),
        })
    }

    /// Generate a new _serverAuth_ certificate signed by the group's CA.
    pub fn server_cert(&self, server_id: InstanceId) -> Result<GroupServerCert> {
        if !server_id.is_type(InstanceType::Server) {
            bail!("A server ID is required");
        }

        // Generate key
        let keypair = KeyPair::generate()?;

        // Generate certificate
        let mut cert_params = CertificateParams::default();
        cert_params
            .extended_key_usages
            .push(ExtendedKeyUsagePurpose::ServerAuth);
        cert_params.not_before = OffsetDateTime::now_utc();
        cert_params.subject_alt_names = vec![SanType::DnsName(
            format!("{server_id}.{}", self.name).try_into()?,
        )];
        // TODO not_after of 1 year

        // Generate the certificate signed by the CA
        let cert = cert_params.signed_by(&keypair, &self.ca()?, &KeyPair::from_pem(&self.key)?)?;

        debug!(cert = ?cert.params(), "Generated new group server certificate");
        Ok(GroupServerCert {
            cert: cert.pem(),
            key: keypair.serialize_pem(),
        })
    }
}

#[cfg(test)]
mod test_group_ca {
    use super::*;
    use io::Write;
    use openssl::{
        ec::EcKey,
        pkey::PKey,
        rsa::Rsa,
        ssl::{
            Ssl, SslAcceptor, SslConnector, SslContextBuilder, SslMethod, SslMode, SslStream,
            SslVerifyMode,
        },
        x509::X509,
    };
    use std::net::{TcpListener, TcpStream};

    #[test]
    fn test_generate_and_authenticate() -> Result<()> {
        let ca = GroupCaCert::new(ClusterId::default(), "default".parse()?)?;
        let client = ca.client_cert()?;
        let server = ca.server_cert(InstanceId::new_server())?;

        // Write CA cert to temp file
        let mut ca_file = tempfile::NamedTempFile::new()?;
        ca_file.write_all(ca.cert.as_bytes())?;

        let mut server_context = SslAcceptor::mozilla_intermediate_v5(SslMethod::tls_server())?;
        server_context.set_verify(SslVerifyMode::PEER);
        server_context.set_ca_file(&ca_file);
        server_context.set_certificate(&&X509::from_pem(server.cert.as_bytes())?)?;
        server_context.set_private_key(&&PKey::from_ec_key(EcKey::private_key_from_pem(
            server.key.as_bytes(),
        )?)?)?;
        let server_context = server_context.build();

        let mut client_context = SslConnector::builder(SslMethod::tls_client())?;
        client_context.set_verify(SslVerifyMode::PEER);
        client_context.set_ca_file(&ca_file);
        client_context.set_certificate(&&X509::from_pem(client.cert.as_bytes())?)?;
        client_context.set_private_key(&&PKey::from_ec_key(EcKey::private_key_from_pem(
            client.key.as_bytes(),
        )?)?)?;
        let client_context = client_context.build();

        // Start temporary server and listen for one connection
        std::thread::spawn(move || {
            let listener = TcpListener::bind("127.0.0.1:9999").unwrap();
            for stream in listener.incoming() {
                let mut ssl = server_context.accept(stream.unwrap()).unwrap();
                ssl.do_handshake().unwrap();
                break;
            }
        });

        // Make connection
        let stream = TcpStream::connect("127.0.0.1:9999")?;
        let mut ssl = client_context.connect(&server.subject_name()?, stream)?;
        ssl.do_handshake()?;

        Ok(())
    }
}

#[derive(Debug, Clone)]
pub struct TlsData {
    peer_certificates: Option<Vec<CertificateDer<'static>>>,
}

#[derive(Debug, Clone)]
pub struct GroupAcceptor(RustlsAcceptor);

impl GroupAcceptor {
    pub fn new(groups: Collection<GroupData>) -> Result<Self> {
        let mut roots = RootCertStore::empty();
        let mut sni_resolver = ResolvesServerCertUsingSni::new();

        let config = ServerConfig::builder();

        for group in groups.documents() {
            let group = group?;
            let ca: Document<GroupCaCert> = group.get_document("ca")?.unwrap();
            let server: Document<GroupServerCert> = group.get_document("server")?.unwrap();

            roots.add(pem::parse(&ca.data.cert)?.into_contents().try_into()?)?;

            let private_key = config
                .crypto_provider()
                .key_provider
                .load_private_key(PrivateKeyDer::from_pem_slice(&server.data.key.as_bytes())?)?;

            sni_resolver.add(
                &server.data.subject_name()?,
                rustls::sign::CertifiedKey::new(
                    vec![pem::parse(&server.data.cert)?.into_contents().try_into()?],
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
        request
            .extensions_mut()
            .insert(cn.parse::<GroupName>().map_err(|_| "Invalid group name")?);
    } else {
        return Err("missing client certificate");
    }

    Ok(next.run(request).await)
}
