use crate::server::LocationService;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct LocationConfig {
    /// Service to use for resolving IP location info
    #[cfg(feature = "server")]
    pub service: LocationService,
}
