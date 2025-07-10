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

    if !config.disable_control_socket {
        info!(path = %config.agent.socket.display(), "Starting agent control socket");
        let uds = tokio::net::UnixListener::bind(&config.agent.socket)?;
        let handle = tokio::spawn(async move {
            axum::serve(uds, app.with_state(state).into_make_service()).await
        });
        tokio::select! {
            _ = network.run() => {
                bail!("Failed to run connection routines");
            }
            result = handle => {
                result?;
            }
        };
    } else {
    }
}
