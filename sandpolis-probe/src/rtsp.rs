use serde::Deserialize;
use serde::Serialize;

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct RtspConfig {
    pub port: u16,
    pub username: String,
    pub password: String,
    pub path: String,
}
