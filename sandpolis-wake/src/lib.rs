use anyhow::Result;
use sandpolis_core::InstanceId;
use sandpolis_core::RealmName;
use sandpolis_network::NetworkLayer;
use serde::{Deserialize, Serialize};

#[cfg(feature = "client")]
pub mod client;

#[derive(Clone)]
pub struct WakeLayer {
    pub network: NetworkLayer,
}

impl WakeLayer {
    // pub async fn schedule(&self, id: InstanceId, request: WakeRequest) -> Result<WakeResponse> {
    //     todo!()
    // }
}

#[derive(Serialize, Deserialize)]
pub enum WakeAction {
    Poweroff,
    Reboot,
}

pub enum WakePermission {
    Poweroff(Vec<RealmName>),
    Reboot(Vec<RealmName>),
}
