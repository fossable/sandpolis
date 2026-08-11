use crate::{InstanceState, config::Configuration};
use anyhow::Result;
use axum::{
    Router,
    routing::{get, post},
};
use rand::RngExt;
use sandpolis_instance::realm::RealmClusterCert;
use sandpolis_instance::{ClusterId, InstanceId};
use std::path::PathBuf;
use tempfile::TempDir;
use tempfile::tempdir;
use tower_http::trace::TraceLayer;
use tracing::info;

pub async fn main(config: Configuration, state: InstanceState) -> Result<()> {
    #[cfg(feature = "layer-account")]
    // Before the services start, so the first favicon sweep sees every
    // configured domain.
    state.account.seed_accounts(&config.account)?;

    // Every layer's server-side background work goes on one runner, which owns
    // the schedules and lets a client enable or disable individual services.
    let mut services = sandpolis_instance::service::ServiceRunner::new(
        state.instance.realm().clone(),
        state.instance.instance_id,
    );

    // Scraping third-party data (favicons, etc) is server-only: agents have no
    // reason to reach out on the estate's behalf, and every client doing it
    // independently would just multiply the traffic.
    #[cfg(feature = "layer-account")]
    state
        .account
        .register_services(&config.account, &mut services)?;

    services.start()?;

    // A local stratum server can't serve TLS until the global stratum server has
    // issued its certificate, so this blocks (with retries) before binding.
    sandpolis_server::stratum::enroll(&state.server, &state.instance).await?;

    // The global stratum server claims its own attached instances against the
    // grant table; this is what revokes a local stratum server when an agent
    // moves here.
    if state.server.stratum.is_global() {
        tokio::spawn(sandpolis_server::ownership::maintain_local_claims(
            state.server.ownership.clone(),
            state.network.clone(),
            state.instance.instance_id,
        ));
    }

    // A local stratum server holds a link to its global stratum server for as
    // long as it runs: estate data replicates down it, ownership is claimed up
    // it, and it is the default route for anything not attached here.
    if state.server.stratum.is_local() {
        let server = state.server.clone();
        let network = state.network.clone();
        let instance = state.instance.clone();
        tokio::spawn(async move {
            if let Err(e) = sandpolis_server::stratum::maintain_upstream(server, network, instance)
                .await
            {
                tracing::error!(error = %e, "Upstream stratum link stopped");
            }
        });

        // Pull each owned, attached agent's records into the local database.
        // Independent of the upstream link, so agents keep syncing while the
        // global stratum server is unreachable.
        if let Some(table) = state.server.database.authority().scope_table() {
            tokio::spawn(sandpolis_server::ownership::maintain_agent_sync(
                state.network.clone(),
                state.instance.realm().clone(),
                table.clone(),
                state.server.ownership.clone(),
                state.instance.instance_id,
            ));
        }
    }

    let app: Router<InstanceState> = Router::new();

    // Server layer
    let app: Router<InstanceState> =
        app.route("/server/banner", get(sandpolis_server::banner::get_banner));

    // User layer
    let app: Router<InstanceState> = app.route(
        "/user/login",
        post(sandpolis_server::login::server::post_login),
    );

    // Websocket connection endpoint (clients + agents) for streams / sync
    let app: Router<InstanceState> =
        app.route("/connect", get(sandpolis_server::user::server::connect));

    // Realm layer: the global stratum server issues server certificates to
    // local stratum servers so the whole network shares one trust root.
    let app: Router<InstanceState> = app.route(
        "/realm/server-cert",
        post(sandpolis_server::stratum::issue_server_cert),
    );

    let app = app.route_layer(axum::middleware::from_fn(
        sandpolis_instance::realm::server::auth_middleware,
    ));

    // Reject requests from blocked IPs before authentication runs
    let blocklist =
        sandpolis_server::block::IpBlockList::new(config.server.blocked_ips.iter().copied());
    let app = app.route_layer(axum::middleware::from_fn_with_state(
        blocklist,
        sandpolis_server::block::block_middleware,
    ));

    // Tracing support for Axum
    let app = app.layer(TraceLayer::new_for_http());

    info!(listener = ?config.server.listen, "Starting server listener");
    axum_server::bind(config.server.listen)
        .acceptor(
            sandpolis_instance::realm::server::RealmAcceptor::new(
                state.instance.clone(),
                state.realm.clone(),
            )
            .await?,
        )
        .serve(
            app.clone()
                .with_state(state.clone())
                .into_make_service_with_connect_info::<std::net::SocketAddr>(),
        )
        .await?;
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
    let database =
        sandpolis_instance::database::DatabaseLayer::new(
            config.database.clone(),
            &crate::MODELS,
            sandpolis_instance::database::WriteAuthority::Full,
        )?;

    // Generate temporary certs
    let certs = tempdir()?;
    let ca_cert = RealmClusterCert::new(cluster_id, "test".parse()?)?;
    let _server_cert = ca_cert.server_cert(instance_id)?;
    let client_cert = ca_cert.client_cert()?;
    client_cert.write(certs.path().join("client.cert"))?;

    // Temporary listening port
    let port: u16 = rand::rng().random_range(9000..9999);
    config.server.listen = format!("127.0.0.1:{port}",).parse()?;

    let state = InstanceState::new(
        config.clone(),
        database,
        sandpolis_server::ServerStratum::Global,
    )
    .await?;

    // Spawn the server
    tokio::spawn(async move { main(config, state).await });

    Ok(TestServer {
        port,
        endpoint_cert: certs.path().join("client.cert"),
        certs,
    })
}
