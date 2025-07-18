use crate::{InstanceState, config::Configuration};
use anyhow::{Context, Result, bail};
use axum::{
    Router,
    routing::{get, post},
};
use rand::Rng;
use sandpolis_core::{ClusterId, InstanceId};
use sandpolis_realm::RealmClusterCert;
use std::path::PathBuf;
use tempfile::TempDir;
use tempfile::tempdir;
use tower_http::trace::TraceLayer;
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

    // Tracing support for Axum
    let app = app.layer(TraceLayer::new_for_http());

    info!(listener = ?config.server.listen, "Starting server listener");
    let main_handle = axum_server::bind(config.server.listen)
        .acceptor(
            sandpolis_realm::server::RealmAcceptor::new(
                state.instance.clone(),
                state.realm.clone(),
            )
            .await?,
        )
        .serve(app.clone().with_state(state.clone()).into_make_service());

    if let Some(socket_directory) = &config.instance.socket_directory {
        config.instance.clear_socket_path("server.sock")?;

        info!(
            socket = format!("{}/server.sock", socket_directory.display()),
            "Starting admin socket"
        );
        let uds =
            tokio::net::UnixListener::bind(&format!("{}/server.sock", socket_directory.display()))?;
        let control_handle = tokio::spawn(async move {
            // TODO should admin socket get separate app and state?
            axum::serve(uds, app.with_state(state).into_make_service()).await
        });
        tokio::select! {
            result = main_handle => {
                result?;
            }
            result = control_handle => {
                result??;
            }
        };
    } else {
        main_handle.await?;
    }
    Ok(())
}

/// Holds randomized parameters for a test server.
pub struct TestServer {
    pub port: u16,
    pub endpoint_cert: PathBuf,
    certs: TempDir,
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
    let database = sandpolis_database::DatabaseLayer::new(config.database.clone(), &crate::MODELS)?;

    // Generate temporary certs
    let certs = tempdir()?;
    let ca_cert = RealmClusterCert::new(cluster_id, "test".parse()?)?;
    let server_cert = ca_cert.server_cert(instance_id)?;
    let client_cert = ca_cert.client_cert()?;
    client_cert.write(certs.path().join("client.cert"))?;

    // Temporary listening port
    let port: u16 = rand::rng().random_range(9000..9999);
    config.server.listen = format!("127.0.0.1:{port}",).parse()?;

    let state = InstanceState::new(config.clone(), database).await?;

    // Spawn the server
    tokio::spawn(async move { main(config, state).await });

    Ok(TestServer {
        port,
        endpoint_cert: certs.path().join("client.cert"),
        certs,
    })
}
