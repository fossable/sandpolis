use std::{net::SocketAddr, path::PathBuf, sync::Arc};

use anyhow::Result;
use axum::ServiceExt;
use axum::{routing::get, Router};
use axum_server::tls_rustls::RustlsConfig;
use indradb::{Database, MemoryDatastore};
use tokio::sync::Mutex;
use tracing::debug;

#[derive(Clone)]
pub struct AppState {
    pub db: Arc<Mutex<Database<MemoryDatastore>>>,
}

pub async fn main() -> Result<()> {
    let state = AppState {
        db: Arc::new(Mutex::new(MemoryDatastore::new_db())),
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
    debug!("listening on {}", addr);
    axum_server::bind_rustls(addr, config)
        .serve(app.into_make_service())
        .await
        .unwrap();
    Ok(())
}
