use super::DesktopLayer;
use axum::Router;

impl DesktopLayer {
    pub fn agent_routes<S>() -> Router<S>
    where
        S: Clone + Sync + Send + 'static,
    {
        Router::new()
    }
}
