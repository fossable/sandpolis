use std::{net::SocketAddr, path::PathBuf, sync::Arc};

use anyhow::Result;
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

    let app = Router::new();
    // .with_state(state)
    // .route("/db/*path", get(crate::api::read));

    let config = RustlsConfig::from_pem_file(
        PathBuf::from(env!("CARGO_MANIFEST_DIR"))
            .join("self_signed_certs")
            .join("cert.pem"),
        PathBuf::from(env!("CARGO_MANIFEST_DIR"))
            .join("self_signed_certs")
            .join("key.pem"),
    )
    .await
    .unwrap();

    let addr = SocketAddr::from(([127, 0, 0, 1], 443));
    debug!("listening on {}", addr);
    axum_server::bind_rustls(addr, config)
        .serve(app.into_make_service())
        .await
        .unwrap();
    Ok(())
}
