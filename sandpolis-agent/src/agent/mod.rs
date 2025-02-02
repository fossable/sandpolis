use anyhow::Result;
use axum::{
    extract::{self, State},
    routing::post,
    Json, Router,
};
use axum_macros::debug_handler;
use serde::{Deserialize, Serialize};
use tokio_cron_scheduler::JobScheduler;

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
    pub scheduler: JobScheduler,
}

impl AgentLayer {
    pub async fn new(data: Document<AgentLayerData>) -> Result<Self> {
        Ok(Self {
            data,
            scheduler: JobScheduler::new().await?,
        })
    }
}
