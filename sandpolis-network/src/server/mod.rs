use axum::Json;
use axum::Router;
use axum::extract;
use axum::extract::State;
use axum::routing::post;

/// Refuse requests from IP addresses on a configurable block-list (even if they
/// can authenticate).
pub async fn block_middleware(
    State(state): State<NetworkLayer>,
    mut request: Request,
    next: Next,
) -> Result<Response, &'static str> {
    todo!();
    Ok(next.run(request).await)
}
