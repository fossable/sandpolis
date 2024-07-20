use crate::core::database::Database;
use crate::CommandLine;
use anyhow::Result;
use axum::{
    body::{Body, Bytes},
    extract::{Request, State},
    http::{HeaderMap, HeaderName, HeaderValue, Uri},
    response::{IntoResponse, Response},
    routing::get,
    Router,
};
use axum_macros::debug_handler;
use axum_server::tls_rustls::RustlsConfig;
use clap::Parser;
use std::{net::SocketAddr, path::PathBuf, sync::Arc};
use tracing::{info, trace};

#[derive(Parser, Debug, Clone)]
pub struct ServerCommandLine {
    /// The server listen address:port
    pub listen: Option<String>,
}

#[derive(Clone)]
pub struct AppState {
    pub db: Database,
}

pub async fn main(args: CommandLine) -> Result<()> {
    let state = AppState {
        db: Database::new(None, "test", "test").await?,
    };

    let app = Router::new().fallback(db_proxy).with_state(state);

    let config = RustlsConfig::from_pem_file(
        PathBuf::from("/tmp/cert.pem"),
        PathBuf::from("/tmp/cert.key"),
    )
    .await
    .unwrap();

    let addr = SocketAddr::from(([0, 0, 0, 0], 8768));
    info!("listening on {}", addr);
    axum_server::bind_rustls(addr, config)
        .serve(app.into_make_service())
        .await
        .unwrap();
    Ok(())
}

/// Proxy a request to the local database and return its response
#[debug_handler]
async fn db_proxy(state: State<AppState>, request: Request) -> impl IntoResponse {
    trace!("Passing request to local database");
    let response = state
        .db
        .local
        .request(request.method().to_owned(), "")
        // .body(request.body().to_owned())
        .send()
        .await
        .unwrap();

    let response_builder = Response::builder().status(response.status().as_u16());

    // Here the mapping of headers is required due to reqwest and axum differ on the http crate versions
    let mut headers = HeaderMap::with_capacity(response.headers().len());
    headers.extend(response.headers().into_iter().map(|(name, value)| {
        let name = HeaderName::from_bytes(name.as_ref()).unwrap();
        let value = HeaderValue::from_bytes(value.as_ref()).unwrap();
        (name, value)
    }));

    response_builder
        .body(Body::from_stream(response.bytes_stream()))
        // This unwrap is fine because the body is empty here
        .unwrap()
}
