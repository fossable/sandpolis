#[data(instance)]
pub struct ArpEntryData {
    /// IPv4 address target
    pub address: String,
    /// MAC address of broadcasted address
    pub mac: String,
    /// Interface of the network for the MAC
    pub interface_id: String,
    /// Whether the ARP entry is permanent
    pub permanent: Boolean,
}
