use crate::{InstanceState, config::Configuration};
use anyhow::{Result, bail};
use axum::{
    Router,
    routing::{any, get, post},
};
use tracing::info;

pub async fn main(config: Configuration, state: InstanceState) -> Result<()> {
    let app: Router<InstanceState> = Router::new().route("/versions", get(crate::routes::versions));

    // Agent layer routes
    let app = app.route(
        "/agent/uninstall",
        post(sandpolis_agent::agent::routes::uninstall),
    );

    // Network layer routes
    let app = app.route("/network/ping", get(sandpolis_network::routes::ping));

    // Shell layer routes
    #[cfg(feature = "layer-shell")]
    let app = app
        .route(
            "/shell/session",
            any(sandpolis_shell::agent::routes::session),
        )
        .route(
            "/shell/execute",
            post(sandpolis_shell::agent::routes::execute),
        );

    // Filesystem layer routes
    #[cfg(feature = "layer-filesystem")]
    let app = app
        .route(
            "/filesystem/session",
            any(sandpolis_filesystem::agent::routes::session),
        )
        .route(
            "/filesystem/delete",
            post(sandpolis_filesystem::agent::routes::delete),
        );

    // Package layer routes
    #[cfg(feature = "layer-package")]
    let app = app.nest(
        "/layer/package",
        sandpolis_package::PackageLayer::agent_routes(),
    );

    // Desktop layer routes
    #[cfg(feature = "layer-desktop")]
    let app = app.nest(
        "/layer/desktop",
        sandpolis_desktop::DesktopLayer::agent_routes(),
    );

    let network = state.network.clone();

    if let Some(socket_directory) = &config.instance.socket_directory {
        config.instance.clear_socket_path("agent.sock")?;
        info!(
            socket = format!("{}/agent.sock", socket_directory.display()),
            "Starting admin socket"
        );

        let uds =
            tokio::net::UnixListener::bind(&format!("{}/server.sock", socket_directory.display()))?;
        let handle = tokio::spawn(async move {
            axum::serve(uds, app.with_state(state).into_make_service()).await
        });
        tokio::select! {
            result = handle => {
                result?;
            }
        };
    } else {
    }
    Ok(())
}
