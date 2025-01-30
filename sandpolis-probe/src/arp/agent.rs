use libarp::{arp::ArpMessage, client::ArpClient, interfaces::Interface, interfaces::MacAddr};

struct ArpScanner {
    range: Range<IpAddr>,
}

impl ArpScanner {
    async fn run(&self) {
        let mut client = ArpClient::new().unwrap();

        for ip in self.range.iter() {
            // TODO collect all of these
            let result = client.ip_to_mac(ip_addr, None);
        }
    }
}
