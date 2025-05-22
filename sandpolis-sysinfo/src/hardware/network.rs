#[derive(Serialize, Deserialize, PartialEq, Debug)]
#[native_model(id = 5, version = 1)]
#[native_db]
pub struct NetworkInterfaceData {
    #[primary_key]
    pub _id: u32,

    #[secondary_key]
    pub _instance_id: InstanceId,

    #[secondary_key]
    pub _timestamp: DbTimestamp,

    /// The interface's name
    #[secondary_key]
    pub name: String,
    /// The interface's description
    pub description: String,
    /// The interface's maximum transmission unit in bytes
    pub mtu: u32,
    /// The interface's MAC address
    pub mac: String,
    /// null
    pub r#virtual: Option<bool>,
    /// The interface's IPv4 addresses
    pub ipv4: String,
    /// The interface's IPv6 addresses
    pub ipv6: String,
    /// null
    pub broadcast: String,
    /// The interface's subnet mask
    pub netmask: String,
    /// The number of bytes read from the interface
    pub read_bytes: u64,
    /// The number of bytes written to the interface
    pub write_bytes: u64,
    /// The number of packets read from the interface
    pub read_packets: u64,
    /// The number of packets written to the interface
    pub write_packets: u64,
    /// The number of read errors
    pub read_errors: u64,
    /// The number of write errors
    pub write_errors: u64,
    /// The number of read drops
    pub read_drops: u64,
    /// The number of write drops
    pub write_drops: u64,
    /// The number of write collisions
    pub write_collisions: u64,
    /// The interface's maximum speed in bytes
    pub link_speed: u64,
    /// null
    pub default_gateway: Option<bool>,
    /// null
    pub flag_up: Option<bool>,
    /// null
    pub flag_running: Option<bool>,
    /// Whether the interface is a loopback interface
    pub flag_loopback: Option<bool>,
    /// null
    pub flag_multicast: Option<bool>,
    /// The interface's locally unique identifier
    pub luid: Option<String>,
    /// The interface's globally unique identifier
    pub guid: String,
    /// Whether the interface is in a paused state
    pub paused: Option<bool>,
    /// Whether the interface is in a low-power state
    pub low_power: Option<bool>,
}
