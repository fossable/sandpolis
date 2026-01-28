use super::RealmAgentCert;
use super::RealmClientCert;
use super::RealmClusterCert;
use super::RealmLayer;
use super::RealmName;
use super::RealmServerCert;
use super::RealmServerCertKey;
use anyhow::Result;
use anyhow::anyhow;
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
use sandpolis_instance::ClusterId;
use sandpolis_instance::InstanceId;
use sandpolis_instance::InstanceLayer;
use sandpolis_instance::InstanceType;
use sandpolis_instance::database::DataCondition;
use sandpolis_instance::database::Resident;
use std::io;
use std::sync::Arc;
use time::Duration;
use time::OffsetDateTime;
use tokio::io::{AsyncRead, AsyncWrite};
use tokio_rustls::server::TlsStream;
use tower::Layer;
use tracing::debug;
use tracing::trace;
use x509_parser::prelude::{FromDer, X509Certificate};

impl super::RealmClusterCert {
    /// Generate a new realm CA certificate.
    pub fn new(cluster_id: ClusterId, name: RealmName) -> Result<Self> {
        // Generate key
        let keypair = KeyPair::generate()?;

        // Generate certificate
        let mut cert_params = CertificateParams::default();
        cert_params.is_ca = IsCa::Ca(BasicConstraints::Unconstrained);
        cert_params.not_before = OffsetDateTime::now_utc();
        cert_params.not_after = OffsetDateTime::now_utc().saturating_add(Duration::days(36780));
        cert_params.subject_alt_names = vec![SanType::DnsName(cluster_id.to_string().try_into()?)];

        // Generate the certificate
        let cert = cert_params.self_signed(&keypair)?;

        debug!(cert = ?cert.params(), "Generated new realm CA certificate");
        Ok(Self {
            name,
            cert: cert.der().to_vec(),
            key: Some(keypair.serialize_der()),
            ..Default::default()
        })
    }

    pub fn ca(&self) -> Result<Certificate> {
        // TODO https://github.com/rustls/rcgen/issues/274

        Ok(
            CertificateParams::from_ca_cert_der(&self.cert.clone().try_into()?)?.self_signed(
                &KeyPair::try_from(self.key.clone().ok_or_else(|| anyhow!("No key"))?)?,
            )?,
        )
    }

    /// Generate a new realm certificate for agent instances.
    pub fn agent_cert(&self) -> Result<RealmAgentCert> {
        // Generate key
        let keypair = KeyPair::generate()?;

        // Generate certificate
        let mut cert_params = CertificateParams::default();
        cert_params
            .extended_key_usages
            .push(ExtendedKeyUsagePurpose::ClientAuth);
        cert_params
            .extended_key_usages
            .push(ExtendedKeyUsagePurpose::Other(vec![
                1,
                1,
                1,
                InstanceType::Agent.mask() as u64, // TODO auth middleware must check this
            ]));
        cert_params.not_before = OffsetDateTime::now_utc();
        cert_params.not_after = OffsetDateTime::now_utc().saturating_add(Duration::days(365));
        cert_params
            .distinguished_name
            .push(DnType::CommonName, &*self.name);

        // Generate the certificate signed by the CA
        let cert = cert_params.signed_by(
            &keypair,
            &self.ca()?,
            &KeyPair::try_from(self.key.clone().ok_or_else(|| anyhow!("No key"))?)?,
        )?;

        debug!(cert = ?cert.params(), "Generated new realm agent certificate");
        Ok(RealmAgentCert {
            ca: self.ca()?.der().to_vec(),
            cert: cert.der().to_vec(),
            key: Some(keypair.serialize_der()),
            ..Default::default()
        })
    }

    /// Generate a new realm certificate for client instances.
    pub fn client_cert(&self) -> Result<RealmClientCert> {
        // Generate key
        let keypair = KeyPair::generate()?;

        // Generate certificate
        let mut cert_params = CertificateParams::default();
        cert_params
            .extended_key_usages
            .push(ExtendedKeyUsagePurpose::ClientAuth);
        cert_params
            .extended_key_usages
            .push(ExtendedKeyUsagePurpose::Other(vec![
                1,
                1,
                1,
                InstanceType::Client.mask() as u64, // TODO auth middleware must check this
            ]));
        cert_params.not_before = OffsetDateTime::now_utc();
        cert_params.not_after = OffsetDateTime::now_utc().saturating_add(Duration::days(365));
        cert_params
            .distinguished_name
            .push(DnType::CommonName, &*self.name);

        // Generate the certificate signed by the CA
        let cert = cert_params.signed_by(
            &keypair,
            &self.ca()?,
            &KeyPair::try_from(self.key.clone().ok_or_else(|| anyhow!("No key"))?)?,
        )?;

        debug!(cert = ?cert.params(), "Generated new realm client certificate");
        Ok(RealmClientCert {
            ca: self.ca()?.der().to_vec(),
            cert: cert.der().to_vec(),
            key: Some(keypair.serialize_der()),
            ..Default::default()
        })
    }

    /// Generate a new realm certificate for server instances.
    pub fn server_cert(&self, server_id: InstanceId) -> Result<RealmServerCert> {
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

        // TODO add server id?

        // Can also do client auth when connecting to other servers
        cert_params
            .extended_key_usages
            .push(ExtendedKeyUsagePurpose::ClientAuth);
        cert_params.not_before = OffsetDateTime::now_utc();
        cert_params.not_after = OffsetDateTime::now_utc().saturating_add(Duration::days(365));
        cert_params.subject_alt_names = vec![SanType::DnsName(
            format!("{}.{}", self.cluster_id()?, self.name).try_into()?,
        )];

        // Generate the certificate signed by the CA
        let cert = cert_params.signed_by(
            &keypair,
            &self.ca()?,
            &KeyPair::try_from(self.key.clone().ok_or_else(|| anyhow!("No key"))?)?,
        )?;

        debug!(cert = ?cert.params(), "Generated new realm server certificate");
        Ok(RealmServerCert {
            cert: cert.der().to_vec(),
            key: Some(keypair.serialize_der()),
            _instance_id: server_id,
            ..Default::default()
        })
    }
}

#[cfg(test)]
mod test_realm_ca {
    use super::*;
    use io::Write;
    use openssl::{
        ec::EcKey,
        pkey::PKey,
        ssl::{SslAcceptor, SslConnector, SslMethod, SslVerifyMode},
        x509::X509,
    };
    use pem::{Pem, encode};
    use std::net::{TcpListener, TcpStream};

    #[test]
    fn test_generate_and_authenticate() -> Result<()> {
        let ca = RealmClusterCert::new(ClusterId::default(), "default".parse()?)?;
        let client = ca.client_cert()?;
        let server = ca.server_cert(InstanceId::new_server())?;

        // Write CA cert to temp file
        let mut ca_file = tempfile::NamedTempFile::new()?;
        ca_file.write_all(encode(&Pem::new("CERTIFICATE", ca.cert)).as_bytes())?;

        let mut server_context = SslAcceptor::mozilla_intermediate_v5(SslMethod::tls_server())?;
        server_context.set_verify(SslVerifyMode::PEER);
        server_context.set_ca_file(&ca_file)?;
        server_context.set_certificate(&&X509::from_der(&server.cert)?)?;
        server_context
            .set_private_key(&&PKey::private_key_from_der(server.key.as_ref().unwrap())?)?;
        let server_context = server_context.build();

        let mut client_context = SslConnector::builder(SslMethod::tls_client())?;
        client_context.set_verify(SslVerifyMode::PEER);
        client_context.set_ca_file(&ca_file)?;
        client_context.set_certificate(&&X509::from_der(&client.cert)?)?;
        client_context
            .set_private_key(&&PKey::private_key_from_der(client.key.as_ref().unwrap())?)?;
        let client_context = client_context.build();

        // Start temporary server and listen for one connection
        let server_handle = std::thread::spawn(move || -> Result<()> {
            let listener = TcpListener::bind("127.0.0.1:9999")?;
            for stream in listener.incoming() {
                let mut ssl = server_context.accept(stream?)?;
                ssl.do_handshake()?;
                break;
            }
            Ok(())
        });

        // Give server time to start
        std::thread::sleep(std::time::Duration::from_millis(100));

        // Make connection
        let stream = TcpStream::connect("127.0.0.1:9999")?;
        let mut ssl = client_context.connect(&server.subject_name()?, stream)?;
        ssl.do_handshake()?;

        // Wait for server thread to complete
        server_handle.join().unwrap()?;

        Ok(())
    }
}

#[derive(Debug, Clone)]
pub struct TlsData {
    peer_certificates: Option<Vec<CertificateDer<'static>>>,
}

/// Accepts TLS connections with realm certificates.
#[derive(Debug, Clone)]
pub struct RealmAcceptor(RustlsAcceptor);

impl RealmAcceptor {
    pub async fn new(instance_layer: InstanceLayer, realm_layer: RealmLayer) -> Result<Self> {
        let mut roots = RootCertStore::empty();
        let mut sni_resolver = ResolvesServerCertUsingSni::new();

        let config = ServerConfig::builder();

        for realm in realm_layer.realms.iter() {
            let realm = realm.read();
            let db = realm_layer.realm(realm.name.clone())?;
            trace!(name = *realm.name, "Registering realm with server acceptor");

            // Add cluster cert as a CA cert to the root store
            {
                let cluster_cert: Resident<RealmClusterCert> = db.resident(())?;

                roots.add(cluster_cert.read().cert.clone().try_into()?)?;
            }

            // Add server cert to the SNI resolver
            {
                // TODO don't allow default
                let server_cert: Resident<RealmServerCert> = db.resident(DataCondition::equal(
                    RealmServerCertKey::_instance_id,
                    instance_layer.instance_id,
                ))?;

                let private_key = config.crypto_provider().key_provider.load_private_key(
                    server_cert
                        .read()
                        .key
                        .clone()
                        .ok_or_else(|| anyhow!("No server key"))?
                        .try_into()
                        .map_err(|_| anyhow!("Failed to parse key"))?,
                )?;

                let subject_name = server_cert.read().subject_name()?;

                trace!(subject_name = %subject_name, "Adding SNI resolver");
                sni_resolver.add(
                    &subject_name,
                    rustls::sign::CertifiedKey::new(
                        vec![server_cert.read().cert.clone().try_into()?],
                        private_key,
                    ),
                )?;
            }
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
impl<I, S> Accept<I, S> for RealmAcceptor
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
            let (stream, service) = match acceptor.accept(stream, service).await {
                Ok(result) => result,
                Err(e) => {
                    debug!("TLS accept failed: {}", e);
                    return Err(e);
                }
            };
            let server_conn = stream.get_ref().1;
            let tls_data = TlsData {
                peer_certificates: server_conn.peer_certificates().map(From::from),
            };
            let service = Extension(tls_data).layer(service);

            // TODO check for revoked certificates

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
            .insert(cn.parse::<RealmName>().map_err(|_| "Invalid realm name")?);
    } else {
        return Err("missing client certificate");
    }

    Ok(next.run(request).await)
}
