use crate::{InstanceState, config::Configuration};
use anyhow::{Context, Result};
use axum::{
    Router,
    routing::{get, post},
};
use rand::Rng;
use sandpolis_database::Database;
use sandpolis_instance::{ClusterId, InstanceId};
use sandpolis_realm::RealmClusterCert;
use std::path::PathBuf;
use tempfile::TempDir;
use tempfile::tempdir;
use tracing::info;

pub async fn main(config: Configuration, state: InstanceState) -> Result<()> {
    // TODO fallback routes from client to agent?

    let app: Router<InstanceState> = Router::new().route("/versions", get(crate::routes::versions));

    // Server layer
    let app: Router<InstanceState> = app.route(
        "/server/banner",
        get(sandpolis_server::server::routes::banner),
    );

    // User layer
    let app: Router<InstanceState> =
        app.route("/user/login", post(sandpolis_user::server::routes::login));

    // Network layer
    let app: Router<InstanceState> =
        app.route("/network/ping", get(sandpolis_network::routes::ping));

    // TODO from_fn_with_state for IP blocking
    let app = app.route_layer(axum::middleware::from_fn(
        sandpolis_realm::server::auth_middleware,
    ));

    info!(listener = ?config.server.listen, "Starting server listener");
    axum_server::bind(config.server.listen)
        .acceptor(sandpolis_realm::server::RealmAcceptor::new(
            state.instance.clone(),
            state.realm.clone(),
        )?)
        .serve(app.with_state(state).into_make_service())
        .await
        .context("binding socket")?;

    Ok(())
}

/// Holds randomized parameters for a test server.
pub struct TestServer {
    pub port: u16,
    pub endpoint_cert: PathBuf,
    certs: TempDir,
    db: Database,
}

/// Run a standalone server instance for testing.
pub async fn test_server() -> Result<TestServer> {
    // Get ready to do some cryptography
    rustls::crypto::aws_lc_rs::default_provider()
        .install_default()
        .expect("crypto provider is available");

    let cluster_id = ClusterId::default();
    let instance_id = InstanceId::new_server();

    let mut config = Configuration::default();

    // Create temporary database
    let db = Database::new_ephemeral()?;

    // Generate temporary certs
    let certs = tempdir()?;
    let ca_cert = RealmClusterCert::new(cluster_id, "test".parse()?)?;
    let server_cert = ca_cert.server_cert(instance_id)?;
    let client_cert = ca_cert.client_cert()?;
    client_cert.write(certs.path().join("client.cert"))?;

    // Temporary listening port
    let port: u16 = rand::rng().random_range(9000..9999);
    config.server.listen = format!("127.0.0.1:{port}",).parse()?;

    let state = InstanceState::new(config.clone(), db.clone()).await?;

    // Spawn the server
    tokio::spawn(async move { main(config, state).await });

    Ok(TestServer {
        port,
        endpoint_cert: certs.path().join("client.cert"),
        certs,
        db,
    })
}
