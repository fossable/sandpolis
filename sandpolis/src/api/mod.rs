use axum::{
    extract::{Path, State},
    response::IntoResponse,
};
use axum_macros::debug_handler;

use crate::server::AppState;

pub mod agent;
pub mod listener;
pub mod server;

#[debug_handler]
pub async fn read(State(state): State<AppState>, Path(path): Path<String>) -> impl IntoResponse {
    todo!()
}
