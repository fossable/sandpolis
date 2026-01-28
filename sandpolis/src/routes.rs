use axum::Json;
use sandpolis_instance::LayerName;
use sandpolis_instance::LayerVersion;
use sandpolis_instance::network::RequestResult;
use std::collections::HashMap;

/// Return versions of all supported layers
#[axum_macros::debug_handler]
pub async fn versions() -> RequestResult<HashMap<LayerName, LayerVersion>> {
    Ok(Json(crate::layers()))
}
