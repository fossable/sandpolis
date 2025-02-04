pub(crate) mod messages;

#[cfg(feature = "agent")]
pub mod agent;

#[cfg(feature = "client")]
pub mod client;

pub struct DesktopLayer {}

pub enum DesktopStreamColorMode {
    /// Each pixel encoded in three bytes
    Rgb888,
    /// Each pixel encoded in two bytes
    Rgb565,
    /// Each pixel encoded in one byte
    Rgb332,
}

pub enum DesktopStreamCompressionMode {
    None,
    Zlib,
    Zstd,
}

pub enum DesktopStreamPointerButton {
    Primary,
    Middle,
    Secondary,
    Back,
    Forward,
}

/// List available desktops.
pub struct DesktopListRequest;

pub struct Desktop {
    /// Desktop name
    pub name: String,

    /// Desktop width in pixels
    pub width: u32,

    /// Desktop height in pixels
    pub height: u32,
}

// Response containing all available desktops
pub enum DesktopListResponse {
    Ok(Vec<Desktop>),
}
