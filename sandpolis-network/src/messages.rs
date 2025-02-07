use sandpolis_instance::InstanceId;
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize)]
pub struct PingRequest {
    pub id: InstanceId,
}

#[derive(Serialize, Deserialize)]
pub struct PingResponse {
    pub time: u64,
    pub id: InstanceId,
    pub from: Option<Box<PingResponse>>,
}
