use smoltcp::iface::{EthernetInterfaceBuilder, NeighborCache, Routes};
use smoltcp::phy::{wait as phy_wait, Device, Medium};
use smoltcp::socket::{TcpSocket, TcpSocketBuffer};

pub struct Connection {

    socket: TcpSocket,
    interface: EthernetInterface,
}

impl Connection {

    pub fn new(snp: Snp, server_ip: String, server_port: u16) -> Result<Connection> {

        let neighbor_cache = NeighborCache::new(BTreeMap::new());

        let tcp_rx_buffer = TcpSocketBuffer::new(vec![0; 64]);
        let tcp_tx_buffer = TcpSocketBuffer::new(vec![0; 128]);
        let tcp_socket = TcpSocket::new(tcp_rx_buffer, tcp_tx_buffer);

        snp.mode().current_address()

        let mut iface = EthernetInterfaceBuilder::new(device)
            .ethernet_addr(hw_addr)
            .neighbor_cache(neighbor_cache)
            .ip_addrs(ip_addrs)
            .finalize();

        let tcp_handle = iface.add_socket(tcp_socket);

        let socket = iface.get_socket::<TcpSocket>(tcp_handle);
        socket.connect((server_ip, server_port), 49500).unwrap();

        Connection {

        }
    }
}
