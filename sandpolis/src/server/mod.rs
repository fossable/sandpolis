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

use crate::core::database::Collection;
use crate::core::user::UserData;
use crate::core::{database::Database, S7S_PORT};
use crate::CommandLine;
use anyhow::{Context, Result};
use axum::{
    body::Body,
    extract::{Request, State},
    response::{IntoResponse, Response},
    Router,
};
use axum_macros::debug_handler;
use axum_server::tls_rustls::RustlsConfig;
use clap::Parser;
use std::sync::Arc;
use std::{
    net::{IpAddr, Ipv4Addr, SocketAddr},
    path::PathBuf,
};
use tracing::{info, trace};

pub mod user;

#[derive(Parser, Debug, Clone)]
pub struct ServerCommandLine {
    /// Server listen address:port
    #[clap(long, default_value_t = SocketAddr::new(IpAddr::V4(Ipv4Addr::new(0, 0, 0, 0)), S7S_PORT))]
    pub listen: SocketAddr,

    /// Create user with interactive prompt
    #[clap(long)]
    pub create_user: bool,
}

#[derive(Clone)]
pub struct ServerState {
    pub db: Database,
    pub local: Arc<ServerInstance>,
}

pub struct ServerInstance {
    pub users: Collection<UserData>,
}

pub async fn main(args: CommandLine) -> Result<()> {
    let db = Database::new(args.storage.join("server.db"))?;
    let state = ServerState {
        local: Arc::new(ServerInstance {
            users: db.collection("/server/users")?,
        }),
        db,
    };

    let app = Router::new().fallback(fallback_handler).with_state(state);

    info!(listener = ?args.server_args.listen, "Starting server instance");
    if args.server_args.listen.port() == 8080 {
        axum_server::bind(args.server_args.listen)
            .serve(app.into_make_service())
            .await
            .context("binding socket")?;
    } else {
        let config = RustlsConfig::from_pem_file(
            // TODO args
            PathBuf::from("/tmp/cert.pem"),
            PathBuf::from("/tmp/cert.key"),
        )
        .await?;

        axum_server::bind_rustls(args.server_args.listen, config)
            .serve(app.into_make_service())
            .await
            .context("binding socket")?;
    }
    Ok(())
}

/// Proxy a request to the local database and return its response
#[debug_handler]
async fn fallback_handler(state: State<ServerState>, request: Request) -> impl IntoResponse {
    trace!("Passing request to local database");

    todo!()
}
