use sandpolis_core::Layer;
use axum::Json;
use sandpolis_instance::LayerVersion;
use sandpolis_network::RequestResult;
use std::collections::HashMap;

/// Return versions of all supported layers
#[axum_macros::debug_handler]
pub async fn versions() -> RequestResult<HashMap<Layer, LayerVersion>> {
    Ok(Json(crate::layers()))
}
