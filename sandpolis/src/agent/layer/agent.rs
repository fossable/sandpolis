use anyhow::Result;
use axum::{
    extract::{self, State},
    routing::post,
    Json, Router,
};
use axum_macros::debug_handler;
use serde::{Deserialize, Serialize};

use crate::{
    agent::AgentState,
    core::{
        database::Document,
        layer::agent::{PowerRequest, PowerResponse},
    },
};

#[derive(Serialize, Deserialize, Default)]
pub struct AgentLayerData;

pub struct AgentLayer {
    pub data: Document<AgentLayerData>,
}

impl AgentLayer {
    pub fn new(data: Document<AgentLayerData>) -> Result<Self> {
        Ok(Self { data })
    }
}

#[debug_handler]
async fn power(
    state: State<AgentState>,
    extract::Json(request): extract::Json<PowerRequest>,
) -> Result<Json<PowerResponse>, Json<PowerResponse>> {
    // libc::reboot
    todo!()
}

pub fn router() -> Router<AgentState> {
    Router::new().route("/power", post(power))
}
