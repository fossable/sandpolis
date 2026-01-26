use crate::{InstanceState, config::Configuration};
use anyhow::Result;
use axum::{
    Router,
    routing::{any, get, post},
};
use tracing::info;

pub async fn main(config: Configuration, state: InstanceState) -> Result<()> {
    let app: Router<InstanceState> = Router::new().route("/versions", get(crate::routes::versions));

    let network = state.network.clone();

    // TODO remove the instance socket and just respond to websocket messages
    if let Some(socket_directory) = &config.instance.socket_directory {
        config.instance.clear_socket_path("agent.sock")?;
        info!(
            socket = format!("{}/agent.sock", socket_directory.display()),
            "Starting admin socket"
        );

        let uds =
            tokio::net::UnixListener::bind(format!("{}/agent.sock", socket_directory.display()))?;
        let handle = tokio::spawn(async move {
            axum::serve(uds, app.with_state(state).into_make_service()).await
        });
        tokio::select! {
            result = handle => {
                result?;
            }
        };
    }
    Ok(())
}
