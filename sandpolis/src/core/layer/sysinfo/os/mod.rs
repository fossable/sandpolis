pub mod memory;
pub mod network;
pub mod process;
pub mod user;

pub struct OsData {
    /// Distribution name
    pub name: String,
    /// The operating system's family
    pub family: os_info::Type,
    /// The operating system's manufacturer
    pub manufacturer: String,
    /// The operating system's register width in bits
    pub bitness: os_info::Bitness,
    /// The operating system's primary version
    pub version: String,
    /// Major release version
    pub major_version: Option<String>,
    /// Minor release version
    pub minor_version: Option<String>,
    /// The operating system's code name
    pub codename: Option<String>,
    /// The operating system's build number
    pub build_number: Option<String>,
    /// OS architecture
    pub arch: String,
}
