use anyhow::{Context, Result};
use axum::{
    routing::{get, post},
    Router,
};
use sandpolis::{config::Configuration, InstanceState};
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
        sandpolis_group::server::auth_middleware,
    ));

    info!(listener = ?config.server.listen, "Starting server listener");
    axum_server::bind(config.server.listen)
        .acceptor(sandpolis_group::server::GroupAcceptor::new(
            state.group.groups.clone(),
        )?)
        .serve(app.with_state(state).into_make_service())
        .await
        .context("binding socket")?;

    Ok(())
}
