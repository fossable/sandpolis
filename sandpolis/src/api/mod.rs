use axum::{
    extract::{Path, State},
    response::IntoResponse,
};

use crate::server::AppState;

pub mod agent;
pub mod listener;
pub mod server;

pub async fn read(State(state): State<AppState>, Path(path): Path<String>) -> impl IntoResponse {
    // wildcard path
    todo!()
}
