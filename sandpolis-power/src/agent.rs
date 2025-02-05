use super::messages::PowerRequest;
use super::messages::PowerResponse;
use super::PowerLayer;
use axum::extract;
use axum::extract::State;
use axum::routing::post;
use axum::Json;
use axum::Router;

#[axum_macros::debug_handler]
async fn power(
    state: State<PowerLayer>,
    extract::Json(request): extract::Json<PowerRequest>,
) -> Result<Json<PowerResponse>, Json<PowerResponse>> {
    // libc::reboot
    todo!()
}

pub fn router() -> Router<PowerLayer> {
    Router::new().route("/power", post(power))
}
