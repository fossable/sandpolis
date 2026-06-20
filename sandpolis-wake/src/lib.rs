use macaddr::MacAddr6;
use sandpolis_instance::network::NetworkLayer;
use sandpolis_instance::realm::RealmName;
use sandpolis_probe::wol::{WolPacketRequest, WolPacketResponse, send_wol_packet};
use serde::{Deserialize, Serialize};

#[cfg(feature = "client")]
pub mod client;

#[derive(Clone)]
pub struct WakeLayer {
    pub network: NetworkLayer,
}

impl WakeLayer {
    /// Send a Wake-on-LAN magic packet to wake the device with the given MAC
    /// address. Reuses the magic-packet implementation from the probe layer.
    pub fn wake(&self, mac: MacAddr6) -> WolPacketResponse {
        send_wol_packet(&WolPacketRequest {
            mac_address: mac,
            broadcast_address: None,
            port: None,
        })
    }
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
