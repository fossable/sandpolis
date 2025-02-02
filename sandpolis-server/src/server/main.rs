use crate::core::database::Database;
use crate::core::database::{Collection, Document};
use crate::core::layer::server::group::GroupCaCert;
use crate::core::layer::server::ServerAddress;
use crate::core::ClusterId;
use crate::server::layer::server::ServerLayer;
use crate::{CommandLine, Commands};
use anyhow::{Context, Result};
use axum::{
    body::Body,
    extract::{FromRef, Request, State},
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
    #[clap(long, default_value_t = SocketAddr::new(IpAddr::V4(Ipv4Addr::new(0, 0, 0, 0)), ServerAddress::default_port()))]
    pub listen: SocketAddr,

    /// Run a local stratum (LS) server instead of global stratum (GS).
    #[clap(long)]
    pub local: bool,
}

#[derive(Clone, FromRef)]
pub struct ServerState {
    pub db: Database,
    pub group: GroupState,
    pub user: UserState,
    pub banner: Document<ServerBanner>,
}

pub async fn main(args: CommandLine) -> Result<()> {
    let db = Database::new(args.storage.join("server.db"))?;

    // Determine cluster ID
    let cluster_id: ClusterId = if let Some(document) = db.get_document("/cluster_id")? {
        // Use saved cluster ID
        document.data
    } else {
        if let Some(servers) = args.server {
            // TODO start connecting to other servers to get cluster ID?
            todo!()
        } else {
            // Generate a new one
            ClusterId::default()
        }
    };

    let state = ServerState {
        server: Arc::new(ServerLayer::new(cluster_id, db.document("server")?)?),
        db,
    };

    let groups = state.server.groups.clone();

    // Dispatch subcommands
    match args.command {
        Some(Commands::GenerateCert { group, output }) => {
            let g = groups.get_document(&group)?.expect("the group exists");
            let ca: Document<GroupCaCert> = g.get_document("ca")?.expect("the CA exists");

            let cert = ca.data.client_cert(&group.parse()?)?;

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
async fn banner(
    state: State<ServerState>,
    extract::Json(_): extract::Json<GetBannerRequest>,
) -> RequestResult<GetBannerResponse> {
    Ok(Json(GetBannerResponse::Ok(state.banner.data.clone())))
}

#[debug_handler]
async fn fallback_handler(state: State<ServerState>, request: Request) -> impl IntoResponse {
    todo!()
}
