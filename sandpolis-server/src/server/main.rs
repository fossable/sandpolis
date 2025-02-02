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
