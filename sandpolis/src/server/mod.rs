use crate::core::database::Database;
use crate::CommandLine;
use anyhow::Result;
use axum::{
    body::Body,
    extract::{Request, State},
    response::{IntoResponse, Response},
    Router,
};
use axum_macros::debug_handler;
use axum_server::tls_rustls::RustlsConfig;
use clap::Parser;
use std::{net::SocketAddr, path::PathBuf};
use tracing::{info, trace};

#[derive(Parser, Debug, Clone, Default)]
pub struct ServerCommandLine {
    /// The server listen address:port
    #[clap(long)]
    pub listen: Option<String>,
}

#[derive(Clone)]
pub struct AppState {
    pub db: Database,
}

pub async fn main(args: CommandLine) -> Result<()> {
    // Use given listen address or default
    let listen: SocketAddr = args
        .server_args
        .listen
        .unwrap_or("0.0.0.0:8768".into())
        .parse()?;

    let state = AppState {
        db: Database::new(None, "test", "test").await?,
    };

    let app = Router::new().fallback(db_proxy).with_state(state);

    if listen.port() == 80 {
        info!(socket = ?listen, "Starting plaintext listener");
        axum_server::bind(listen)
            .serve(app.into_make_service())
            .await?;
    } else {
        let config = RustlsConfig::from_pem_file(
            PathBuf::from("/tmp/cert.pem"),
            PathBuf::from("/tmp/cert.key"),
        )
        .await?;

        info!(socket = ?listen, "Starting TLS listener");
        axum_server::bind_rustls(listen, config)
            .serve(app.into_make_service())
            .await?;
    }
    Ok(())
}

/// Proxy a request to the local database and return its response
#[debug_handler]
async fn db_proxy(state: State<AppState>, request: Request) -> impl IntoResponse {
    trace!("Passing request to local database");

    let response = state
        .db
        .local
        .req(request.method().to_owned(), request.uri().path(), None)
        .headers(request.headers().to_owned())
        // TODO figure out how to stream
        .body(
            axum::body::to_bytes(request.into_body(), usize::MAX)
                .await
                .unwrap(),
        )
        .send()
        .await
        .unwrap();

    let mut response_builder = Response::builder().status(response.status().as_u16());

    for (header, value) in response.headers().into_iter() {
        response_builder = response_builder.header(header, value);
    }

    response_builder
        .body(Body::from_stream(response.bytes_stream()))
        // This unwrap is fine because the body is empty here
        .unwrap()
}
