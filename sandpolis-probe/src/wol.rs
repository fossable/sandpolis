use macaddr::MacAddr6;
use serde::{Deserialize, Serialize};
use std::net::{IpAddr, Ipv4Addr, SocketAddr, UdpSocket};

/// Build a Wake-on-LAN magic packet (102 bytes).
///
/// The magic packet consists of:
/// - 6 bytes of 0xFF (synchronization stream)
/// - 16 repetitions of the target MAC address (96 bytes)
pub fn magic_packet(mac: &MacAddr6) -> [u8; 102] {
    let mut packet = [0xFFu8; 102];
    let bytes = mac.as_ref();

    // Fill bytes 6-101 with 16 repetitions of the MAC address
    for i in 0..16 {
        let offset = 6 + i * 6;
        packet[offset..offset + 6].copy_from_slice(bytes);
    }

    packet
}

/// Request to send a Wake-on-LAN magic packet.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub struct WolPacketRequest {
    /// The target device's MAC address.
    pub mac_address: MacAddr6,

    /// Optional broadcast address to send the packet to.
    /// Defaults to 255.255.255.255 if not specified.
    pub broadcast_address: Option<String>,

    /// Optional port to send the packet to.
    /// Defaults to 9 (discard protocol) if not specified.
    pub port: Option<u16>,
}

/// Response from a Wake-on-LAN packet send operation.
#[derive(Clone, Debug, Serialize, Deserialize)]
pub enum WolPacketResponse {
    /// Packet was sent successfully.
    Ok,
    /// The broadcast address was invalid.
    InvalidBroadcastAddress(String),
    /// Failed to send the packet.
    SendFailed(String),
}

/// Send a Wake-on-LAN magic packet to wake a device.
pub fn send_wol_packet(request: &WolPacketRequest) -> WolPacketResponse {
    let broadcast_addr: IpAddr = match &request.broadcast_address {
        Some(addr) => match addr.parse() {
            Ok(ip) => ip,
            Err(_) => return WolPacketResponse::InvalidBroadcastAddress(addr.clone()),
        },
        None => IpAddr::V4(Ipv4Addr::BROADCAST),
    };

    let port = request.port.unwrap_or(9);
    let dest = SocketAddr::new(broadcast_addr, port);

    // Bind to any available port on all interfaces
    let bind_addr = match broadcast_addr {
        IpAddr::V4(_) => SocketAddr::new(IpAddr::V4(Ipv4Addr::UNSPECIFIED), 0),
        IpAddr::V6(_) => SocketAddr::new(IpAddr::V6(std::net::Ipv6Addr::UNSPECIFIED), 0),
    };

    let socket = match UdpSocket::bind(bind_addr) {
        Ok(s) => s,
        Err(e) => return WolPacketResponse::SendFailed(format!("failed to bind socket: {}", e)),
    };

    // Enable broadcast
    if let Err(e) = socket.set_broadcast(true) {
        return WolPacketResponse::SendFailed(format!("failed to enable broadcast: {}", e));
    }

    let packet = magic_packet(&request.mac_address);

    match socket.send_to(&packet, dest) {
        Ok(_) => WolPacketResponse::Ok,
        Err(e) => WolPacketResponse::SendFailed(format!("failed to send packet: {}", e)),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_magic_packet() {
        let mac: MacAddr6 = "AA:BB:CC:DD:EE:FF".parse().unwrap();
        let packet = magic_packet(&mac);

        // Check header (6 bytes of 0xFF)
        assert_eq!(&packet[0..6], &[0xFF; 6]);

        // Check that MAC address is repeated 16 times
        let mac_bytes = mac.as_ref();
        for i in 0..16 {
            let offset = 6 + i * 6;
            assert_eq!(&packet[offset..offset + 6], mac_bytes);
        }

        // Check total length
        assert_eq!(packet.len(), 102);
    }
}
