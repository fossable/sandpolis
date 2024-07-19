use crate::core::database::Database;
use crate::CommandLine;
use anyhow::Result;
use axum::{routing::get, Router};
use axum_server::tls_rustls::RustlsConfig;
use clap::Parser;
use std::{net::SocketAddr, path::PathBuf, sync::Arc};
use tracing::info;

#[derive(Parser, Debug, Clone)]
pub struct ServerCommandLine {
    /// The server listen address:port
    pub listen: Option<Vec<String>>,
}

#[derive(Clone)]
pub struct AppState {
    pub db: Database,
}

pub async fn main(args: CommandLine) -> Result<()> {
    let state = AppState {
        db: Database::new(None, "test", "test").await?,
    };

    let app = Router::new()
        .route("/db/*path", get(crate::api::read))
        .with_state(state);

    let config = RustlsConfig::from_pem_file(
        PathBuf::from("/tmp/cert.pem"),
        PathBuf::from("/tmp/cert.key"),
    )
    .await
    .unwrap();

    let addr = SocketAddr::from(([127, 0, 0, 1], 8443));
    info!("listening on {}", addr);
    axum_server::bind_rustls(addr, config)
        .serve(app.into_make_service())
        .await
        .unwrap();
    Ok(())
}
