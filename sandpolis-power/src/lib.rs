use anyhow::Result;
use messages::{PowerRequest, PowerResponse};
use sandpolis_instance::InstanceId;
use sandpolis_network::NetworkLayer;
use serde::{Deserialize, Serialize};

#[cfg(feature = "agent")]
pub mod agent;
#[cfg(feature = "client")]
pub mod client;

pub mod messages;

#[derive(Clone)]
pub struct PowerLayer {
    pub network: NetworkLayer,
}

impl PowerLayer {
    pub async fn schedule(&self, id: InstanceId, request: PowerRequest) -> Result<PowerResponse> {
        todo!()
    }
}

#[derive(Serialize, Deserialize)]
pub enum PowerAction {
    Poweroff,
    Reboot,
}

pub enum PowerPermission {
    Poweroff(Vec<GroupName>),
    Reboot(Vec<GroupName>),
}
