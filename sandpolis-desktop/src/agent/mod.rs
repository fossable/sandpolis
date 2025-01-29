use axum::Router;

use crate::agent::AgentState;

pub fn router() -> Router<AgentState> {
    Router::new()
}
