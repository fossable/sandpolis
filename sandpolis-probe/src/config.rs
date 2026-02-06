use serde::Deserialize;
use serde::Serialize;
use std::net::IpAddr;

use crate::rtsp::RtspConfig;

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct ProbeLayerConfig {
    devices: Vec<DeviceConfig>,
}

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct DeviceConfig {
    ip: IpAddr,
    rtsp: Option<RtspConfig>,
}
