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

use crate::core::database::{Collection, Document};
use crate::core::layer::server::group::GroupCaCert;
use crate::core::{database::Database, S7S_PORT};
use crate::server::layer::server::ServerLayer;
use crate::{CommandLine, Commands};
use anyhow::{Context, Result};
use axum::{
    body::Body,
    extract::{Request, State},
    response::{IntoResponse, Response},
    Router,
};
use axum_macros::debug_handler;
use clap::{Parser, Subcommand};
use layer::server::group::GroupAcceptor;
use std::fs::File;
use std::io::Write;
use std::net::{IpAddr, Ipv4Addr, SocketAddr};
use std::sync::Arc;
use tracing::{info, trace};

pub mod layer;

#[derive(Parser, Debug, Clone)]
pub struct ServerCommandLine {
    /// Server listen address:port
    #[clap(long, default_value_t = SocketAddr::new(IpAddr::V4(Ipv4Addr::new(0, 0, 0, 0)), S7S_PORT))]
    pub listen: SocketAddr,

    /// Run a local stratum (LS) server instead of global stratum (GS).
    #[clap(long)]
    pub local: bool,
}

#[derive(Clone)]
pub struct ServerState {
    pub db: Database,
    pub server: Arc<ServerLayer>,
}

pub async fn main(args: CommandLine) -> Result<()> {
    let db = Database::new(args.storage.join("server.db"))?;
    let state = ServerState {
        server: Arc::new(ServerLayer::new(db.metadata()?.id, db.document("server")?)?),
        db,
    };

    let groups = state.server.groups.clone();

    // Dispatch subcommands
    match args.command {
        Some(Commands::GenerateCert { group, output }) => {
            let g = groups.get_document(&group)?.expect("the group exists");
            let ca: Document<GroupCaCert> = g.get_document("ca")?.expect("the CA exists");

            let cert = ca.data.generate_cert(&group)?;

            info!(path = %output.display(), "Writing endpoint certificate");
            let mut output = File::create(output)?;
            output.write_all(&serde_json::to_vec(&cert)?)?;
        }
        // Start listener
        _ => {
            let app: Router<()> = Router::new()
                .fallback(fallback_handler)
                .nest("/server", ServerLayer::router())
                .route_layer(axum::middleware::from_fn(
                    layer::server::group::auth_middleware,
                ))
                .with_state(state);

            info!(listener = ?args.server_args.listen, "Starting server instance");
            axum_server::bind(args.server_args.listen)
                .acceptor(GroupAcceptor::new(groups)?)
                .serve(app.into_make_service())
                .await
                .context("binding socket")?;
        }
    }

    Ok(())
}

#[debug_handler]
async fn fallback_handler(state: State<ServerState>, request: Request) -> impl IntoResponse {
    todo!()
}
