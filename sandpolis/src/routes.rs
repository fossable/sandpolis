use axum::Json;
use sandpolis_instance::Layer;
use sandpolis_instance::LayerVersion;
use sandpolis_network::RequestResult;
use std::collections::HashMap;
use std::sync::LazyLock;

static VERSIONS: LazyLock<HashMap<Layer, LayerVersion>> = LazyLock::new(|| {
    HashMap::from([
    // #[cfg(feature = "layer-sysinfo")]
    // (Layer::Sysinfo, todo!()),
// #[cfg(feature = "agent")]
// (Layer::Agent, todo!()),
// #[cfg(feature = "server")]
// (Layer::Server, todo!()),
// #[cfg(feature = "client")]
// (Layer::Client, todo!()),
])
});

/// Return versions of all supported layers
#[axum_macros::debug_handler]
pub async fn versions() -> RequestResult<&'static HashMap<Layer, LayerVersion>> {
    Ok(Json(&*VERSIONS))
}
