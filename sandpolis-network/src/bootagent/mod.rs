use alloc::boxed::Box;
use alloc::vec::Vec;
use anyhow::Result;
use core::convert::TryInto;
use core::ptr;
use rustls::{Certificate, ClientConfig, ClientConnection, PrivateKey};
use sandpolis_core::RealmName;
use sandpolis_realm::RealmAgentCert;
use smoltcp::iface::{Config, Interface, SocketSet};
use smoltcp::phy::{ChecksumCapabilities, Device, DeviceCapabilities, Medium};
use smoltcp::socket::{TcpSocket, TcpSocketBuffer, tcp};
use smoltcp::time::Instant;
use smoltcp::wire::{EthernetAddress, IpAddress, IpCidr, Ipv4Address, Ipv4Cidr};
use uefi::proto::network::snp::MacAddress;
use uefi::proto::network::snp::Snp;
use uefi::{Result as UefiResult, Status};

const IPV4_PROTOCOL: u16 = 0x0800;
const MAX_PACKET_SIZE: usize = 1500;
const ETHERNET_HEADER_SIZE: usize = 14;

/// SNP Device wrapper for smoltcp
pub struct SnpDevice {
    snp: Snp,
    mac_address: EthernetAddress,
    max_packet_size: usize,
}

impl SnpDevice {
    pub fn new(mut snp: Snp) -> Result<Self> {
        // Initialize the SNP
        snp.start().map_err(NetworkError::from)?;
        snp.initialize(0, 0).map_err(NetworkError::from)?;

        // Set receive filters
        snp.receive_filters(
            uefi::proto::network::snp::ReceiveFlags::UNICAST
                | uefi::proto::network::snp::ReceiveFlags::MULTICAST
                | uefi::proto::network::snp::ReceiveFlags::BROADCAST,
            uefi::proto::network::snp::ReceiveFlags::empty(),
            false,
            None,
        )
        .map_err(NetworkError::from)?;

        // Get MAC address
        let current_addr = snp.mode().current_address();
        let mac_bytes: [u8; 6] = current_addr.0[..6]
            .try_into()
            .map_err(|_| NetworkError::BufferTooSmall)?;
        let mac_address = EthernetAddress(mac_bytes);

        let max_packet_size = snp.mode().max_packet_size() as usize;

        Ok(SnpDevice {
            snp,
            mac_address,
            max_packet_size,
        })
    }

    pub fn mac_address(&self) -> EthernetAddress {
        self.mac_address
    }
}

impl Device for SnpDevice {
    type RxToken<'a>
        = RxToken
    where
        Self: 'a;
    type TxToken<'a>
        = TxToken<'a>
    where
        Self: 'a;

    fn capabilities(&self) -> DeviceCapabilities {
        let mut caps = DeviceCapabilities::default();
        caps.max_transmission_unit = self.max_packet_size;
        caps.max_burst_size = Some(1);
        caps.medium = Medium::Ethernet;
        caps.checksum = ChecksumCapabilities::ignored();
        caps
    }

    fn receive(&mut self, _timestamp: Instant) -> Option<(Self::RxToken<'_>, Self::TxToken<'_>)> {
        let mut header_size = ETHERNET_HEADER_SIZE;
        let mut buffer_size = self.max_packet_size;
        let mut buffer = vec![0u8; buffer_size];

        match self.snp.receive(
            &mut header_size,
            &mut buffer_size,
            &mut buffer,
            ptr::null_mut(),
            ptr::null_mut(),
            ptr::null_mut(),
        ) {
            Ok(_) => {
                // Truncate buffer to actual received size
                buffer.truncate(buffer_size);

                let rx_token = RxToken { buffer };
                let tx_token = TxToken { snp: &mut self.snp };
                Some((rx_token, tx_token))
            }
            Err(_) => None,
        }
    }

    fn transmit(&mut self, _timestamp: Instant) -> Option<Self::TxToken<'_>> {
        Some(TxToken { snp: &mut self.snp })
    }
}

pub struct RxToken {
    buffer: Vec<u8>,
}

impl smoltcp::phy::RxToken for RxToken {
    fn consume<R, F>(mut self, f: F) -> R
    where
        F: FnOnce(&mut [u8]) -> R,
    {
        f(&mut self.buffer)
    }
}

pub struct TxToken<'a> {
    snp: &'a mut Snp,
}

impl<'a> smoltcp::phy::TxToken for TxToken<'a> {
    fn consume<R, F>(self, len: usize, f: F) -> R
    where
        F: FnOnce(&mut [u8]) -> R,
    {
        let mut buffer = vec![0u8; len];
        let result = f(&mut buffer);

        // Get source MAC address
        let src_addr = self.snp.mode().current_address();

        // Attempt to transmit
        if let Err(_) = self.snp.transmit(
            ETHERNET_HEADER_SIZE,
            len,
            &buffer,
            &src_addr as *const MacAddress,
            ptr::null_mut(),
            &IPV4_PROTOCOL as *const u16,
        ) {
            // Log transmission failure if needed
            // In a real implementation, you might want to handle this error
        }

        result
    }
}

/// No-std NetworkLayer implementation for bootagent
pub struct NetworkLayer {
    interface: Interface,
    sockets: SocketSet<'static>,
    device: SnpDevice,
    dhcp_enabled: bool,
}

impl NetworkLayer {
    /// Create a new NetworkLayer with the given SNP device
    pub fn new(device: SnpDevice) -> Result<Self> {
        let mac_addr = device.mac_address();

        let config = Config::new(mac_addr.into());
        let mut interface = Interface::new(config, &mut [], Instant::ZERO);

        // Configure with DHCP initially
        interface.update_ip_addrs(|ip_addrs| {
            ip_addrs
                .push(IpCidr::Ipv4(Ipv4Cidr::new(Ipv4Address::UNSPECIFIED, 0)))
                .map_err(|_| NetworkError::BufferTooSmall)
        })?;

        let sockets = SocketSet::new(Vec::new());

        Ok(NetworkLayer {
            interface,
            sockets,
            device,
            dhcp_enabled: true,
        })
    }

    /// Configure static IP address
    pub fn configure_static_ip(
        &mut self,
        ip: Ipv4Address,
        prefix_len: u8,
        gateway: Option<Ipv4Address>,
    ) -> Result<()> {
        self.interface.update_ip_addrs(|ip_addrs| {
            ip_addrs.clear();
            ip_addrs
                .push(IpCidr::Ipv4(Ipv4Cidr::new(ip, prefix_len)))
                .map_err(|_| NetworkError::BufferTooSmall)
        })?;

        if let Some(gw) = gateway {
            self.interface
                .routes_mut()
                .add_default_ipv4_route(gw)
                .map_err(NetworkError::from)?;
        }

        self.dhcp_enabled = false;
        Ok(())
    }

    /// Poll the network interface
    pub fn poll(&mut self, timestamp: Instant) -> Result<bool> {
        let result = self
            .interface
            .poll(timestamp, &mut self.device, &mut self.sockets);
        result.map_err(NetworkError::from)
    }

    /// Create a new TCP socket
    pub fn create_tcp_socket(&mut self) -> Result<tcp::SocketHandle> {
        let rx_buffer = TcpSocketBuffer::new(vec![0; 4096]);
        let tx_buffer = TcpSocketBuffer::new(vec![0; 4096]);
        let socket = TcpSocket::new(rx_buffer, tx_buffer);

        let handle = self.sockets.add(socket);
        Ok(handle)
    }

    /// Connect to a remote host
    pub fn tcp_connect(
        &mut self,
        handle: tcp::SocketHandle,
        remote_addr: IpAddress,
        remote_port: u16,
        local_port: u16,
    ) -> Result<()> {
        let socket = self.sockets.get_mut::<TcpSocket>(handle);
        socket
            .connect(
                self.interface.context(),
                (remote_addr, remote_port),
                local_port,
            )
            .map_err(NetworkError::from)
    }

    /// Check if TCP socket is connected
    pub fn tcp_is_connected(&self, handle: tcp::SocketHandle) -> bool {
        let socket = self.sockets.get::<TcpSocket>(handle);
        socket.is_open()
    }

    /// Send data over TCP socket
    pub fn tcp_send(&mut self, handle: tcp::SocketHandle, data: &[u8]) -> Result<usize> {
        let socket = self.sockets.get_mut::<TcpSocket>(handle);
        socket.send_slice(data).map_err(NetworkError::from)
    }

    /// Receive data from TCP socket
    pub fn tcp_receive(&mut self, handle: tcp::SocketHandle, buffer: &mut [u8]) -> Result<usize> {
        let socket = self.sockets.get_mut::<TcpSocket>(handle);
        socket.recv_slice(buffer).map_err(NetworkError::from)
    }

    /// Close TCP socket
    pub fn tcp_close(&mut self, handle: tcp::SocketHandle) -> Result<()> {
        let socket = self.sockets.get_mut::<TcpSocket>(handle);
        socket.close();
        Ok(())
    }

    /// Get the current IP address
    pub fn ip_address(&self) -> Option<Ipv4Address> {
        for cidr in self.interface.ip_addrs() {
            if let IpCidr::Ipv4(ipv4_cidr) = cidr {
                let addr = ipv4_cidr.address();
                if !addr.is_unspecified() {
                    return Some(addr);
                }
            }
        }
        None
    }
}

impl RealmAgentCertExt for RealmAgentCert {
    fn to_rustls_config(&self) -> Result<(Vec<Certificate>, PrivateKey)> {
        // Convert DER-encoded certificate
        let cert = Certificate(self.cert.clone());

        // Convert DER-encoded private key
        let key = PrivateKey(
            self.key
                .as_ref()
                .ok_or(NetworkError::TlsError(rustls::Error::InvalidMessage))?
                .clone(),
        );

        Ok((vec![cert], key))
    }

    fn ca_certificate(&self) -> Result<Certificate> {
        Ok(Certificate(self.ca.clone()))
    }
}

/// TLS connection wrapper for no-std environments
pub struct TlsConnection {
    socket_handle: tcp::SocketHandle,
    tls_conn: ClientConnection,
}

impl TlsConnection {
    /// Create a new TLS connection with mandatory RealmAgentCert authentication
    pub fn new(
        network: &mut NetworkLayer,
        server_name: &str,
        remote_addr: IpAddress,
        remote_port: u16,
        local_port: u16,
        agent_cert: &RealmAgentCert,
    ) -> Result<Self> {
        // Validate certificate before use
        agent_cert.validate_for_bootagent()?;

        // Create TCP socket
        let socket_handle = network.create_tcp_socket()?;

        // Connect TCP socket
        network.tcp_connect(socket_handle, remote_addr, remote_port, local_port)?;

        // Wait for connection
        let mut attempts = 0;
        while !network.tcp_is_connected(socket_handle) && attempts < 100 {
            network.poll(Instant::ZERO)?;
            attempts += 1;
        }

        if !network.tcp_is_connected(socket_handle) {
            return Err(NetworkError::ConnectionFailed);
        }

        // Create TLS configuration with realm CA certificate
        let mut root_store = rustls::RootCertStore::empty();

        // Add the realm's CA certificate for server validation
        let ca_cert = agent_cert.ca_certificate()?;
        root_store
            .add(&ca_cert)
            .map_err(|_| NetworkError::TlsError(rustls::Error::InvalidMessage))?;

        // Get client certificate and key for mTLS
        let (cert_chain, private_key) = agent_cert.to_rustls_config()?;

        // Configure mandatory client authentication with realm certificates
        let config = ClientConfig::builder()
            .with_root_certificates(root_store)
            .with_client_auth_cert(cert_chain, private_key)
            .map_err(NetworkError::from)?;

        // Create TLS connection
        let server_name = rustls::pki_types::ServerName::try_from(server_name)
            .map_err(|_| NetworkError::TlsError(rustls::Error::InvalidMessage))?;
        let tls_conn = ClientConnection::new(Box::leak(Box::new(config)), server_name)
            .map_err(NetworkError::from)?;

        Ok(TlsConnection {
            socket_handle,
            tls_conn,
        })
    }

    /// Send data over TLS
    pub fn send(&mut self, network: &mut NetworkLayer, data: &[u8]) -> Result<usize> {
        // Write to TLS connection
        self.tls_conn
            .writer()
            .write_all(data)
            .map_err(|_| NetworkError::TlsError(rustls::Error::InvalidMessage))?;

        // Process TLS output
        let mut total_sent = 0;
        while self.tls_conn.wants_write() {
            let mut buffer = [0u8; 1024];
            let len = self
                .tls_conn
                .write_tls(&mut buffer.as_mut_slice())
                .map_err(NetworkError::from)?;

            if len > 0 {
                let sent = network.tcp_send(self.socket_handle, &buffer[..len])?;
                total_sent += sent;
            }
        }

        Ok(total_sent)
    }

    /// Receive data over TLS
    pub fn receive(&mut self, network: &mut NetworkLayer, buffer: &mut [u8]) -> Result<usize> {
        // Read from TCP socket
        let mut tcp_buffer = [0u8; 1024];
        let received = network.tcp_receive(self.socket_handle, &mut tcp_buffer)?;

        if received > 0 {
            // Process TLS input
            self.tls_conn
                .read_tls(&mut &tcp_buffer[..received])
                .map_err(NetworkError::from)?;

            let _ = self
                .tls_conn
                .process_new_packets()
                .map_err(NetworkError::from)?;

            // Read decrypted data
            match self.tls_conn.reader().read(buffer) {
                Ok(len) => Ok(len),
                Err(_) => Ok(0),
            }
        } else {
            Ok(0)
        }
    }

    /// Close TLS connection
    pub fn close(&mut self, network: &mut NetworkLayer) -> Result<()> {
        self.tls_conn.send_close_notify();

        // Send any remaining TLS data
        while self.tls_conn.wants_write() {
            let mut buffer = [0u8; 1024];
            let len = self
                .tls_conn
                .write_tls(&mut buffer.as_mut_slice())
                .map_err(NetworkError::from)?;

            if len > 0 {
                network.tcp_send(self.socket_handle, &buffer[..len])?;
            }
        }

        network.tcp_close(self.socket_handle)
    }
}

/// HTTP client for making simple requests
pub struct HttpClient;

impl HttpClient {
    /// Make a simple GET request over HTTPS with mandatory RealmAgentCert
    pub fn get_https(
        network: &mut NetworkLayer,
        host: &str,
        path: &str,
        port: u16,
        agent_cert: &RealmAgentCert,
        remote_addr: IpAddress, // Must provide actual IP since no DNS resolution
    ) -> Result<Vec<u8>> {
        let mut tls_conn = TlsConnection::new(network, host, remote_addr, port, 0, agent_cert)?;

        // Send HTTP request
        let request = alloc::format!(
            "GET {} HTTP/1.1\r\nHost: {}\r\nConnection: close\r\n\r\n",
            path,
            host
        );
        tls_conn.send(network, request.as_bytes())?;

        // Read response
        let mut response = Vec::new();
        let mut buffer = [0u8; 1024];
        loop {
            let received = tls_conn.receive(network, &mut buffer)?;
            if received == 0 {
                break;
            }
            response.extend_from_slice(&buffer[..received]);
        }

        tls_conn.close(network)?;
        Ok(response)
    }

    /// Make a simple POST request over HTTPS with mandatory RealmAgentCert
    pub fn post_https(
        network: &mut NetworkLayer,
        host: &str,
        path: &str,
        port: u16,
        body: &[u8],
        content_type: &str,
        agent_cert: &RealmAgentCert,
        remote_addr: IpAddress,
    ) -> Result<Vec<u8>> {
        let mut tls_conn = TlsConnection::new(network, host, remote_addr, port, 0, agent_cert)?;

        // Send HTTP POST request
        let request = alloc::format!(
            "POST {} HTTP/1.1\r\nHost: {}\r\nContent-Type: {}\r\nContent-Length: {}\r\nConnection: close\r\n\r\n",
            path,
            host,
            content_type,
            body.len()
        );

        tls_conn.send(network, request.as_bytes())?;
        tls_conn.send(network, body)?;

        // Read response
        let mut response = Vec::new();
        let mut buffer = [0u8; 1024];
        loop {
            let received = tls_conn.receive(network, &mut buffer)?;
            if received == 0 {
                break;
            }
            response.extend_from_slice(&buffer[..received]);
        }

        tls_conn.close(network)?;
        Ok(response)
    }
}
