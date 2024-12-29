//! ## Server Strata
//!
//! There can be multiple servers in a Sandpolis network which improves both
//! scalability and failure tolerance. There are two roles in which a server can
//! exist: a global level and a local level.
//!
//! ### Global Stratum (GS)
//!
//! By default, servers exist in the global stratum. Every network must have at
//! least one GS server and every GS server maintains a persistent connection to
//! every other GS server (fully-connected graph).
//!
//! Every GS server maintains a database containing the entire contents of the
//! network.
//!
//! ### Local Stratum (LS)
//!
//! Local stratum servers are used to serve localized regions and can operate
//! independently if all GS servers becomes unreachable.
//!
//! Every LS server maintains a database containing just the contents relevant to it
//! which is continuously replicated to a GS server's database.

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

    /// Create user with interactive prompt
    #[clap(long)]
    pub create_user: bool,
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
        db: Database::new("test")?,
    };

    let app = Router::new().fallback(fallback_handler).with_state(state);

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
async fn fallback_handler(state: State<AppState>, request: Request) -> impl IntoResponse {
    trace!("Passing request to local database");

    todo!()
}
