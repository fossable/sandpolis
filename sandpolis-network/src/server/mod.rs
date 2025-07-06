use crate::NetworkLayer;
use axum::Json;
use axum::extract::State;
use axum::middleware::Next;
use axum::response::Response;

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
