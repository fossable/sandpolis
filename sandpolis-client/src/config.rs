use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct ClientLayerConfig {
    pub fps: u32,
}

impl Default for ClientLayerConfig {
    fn default() -> Self {
        Self { fps: 30 }
    }
}
