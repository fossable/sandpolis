use serde::Deserialize;
use serde::Serialize;

#[derive(Serialize, Deserialize, Debug, Clone, Default)]
pub struct AccountLayerConfig {
    accounts: Vec<AccountConfig>,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct AccountConfig {
    username: Option<String>,
}
