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

pub struct DesktopStreamRequest {
    /// The desktop to capture
    pub desktop_uuid: String,

    /// The screen scale factor
    pub scale_factor: f64,
}

pub enum DesktopStreamResponse {
    Ok(u64),
}

pub struct DesktopStreamInputEvent {
    /// Indicates a key was pressed
    pub key_pressed: Option<char>,

    /// Indicates a key was released
    pub key_released: Option<char>,

    /// Indicates a key was typed
    pub key_typed: Option<char>,

    /// Indicates a pointing device was pressed
    pub pointer_pressed: Option<DesktopStreamPointerButton>,

    /// Indicates a pointing device was released
    pub pointer_released: Option<DesktopStreamPointerButton>,

    /// The X coordinate of the pointer
    pub pointer_x: Option<i32>,

    /// The Y coordinate of the pointer
    pub pointer_y: Option<i32>,

    /// Screen scale factor
    pub scale_factor: Option<f64>,

    /// Clipboard data
    pub clipboard: Option<Vec<u8>>,
}

pub struct DesktopStreamOutputEvent {
    /// The width of the destination block in pixels
    pub width: Option<i32>,

    /// The height of the destination block in pixels
    pub height: Option<i32>,

    /// The X coordinate of the destination block's top left corner
    pub dest_x: Option<i32>,

    /// The Y coordinate of the destination block's top left corner
    pub dest_y: Option<i32>,

    /// The X coordinate of the source block's top left corner
    pub source_x: Option<i32>,

    /// The Y coordinate of the source block's top left corner
    pub source_y: Option<i32>,

    /// The pixel data encoded according to the session's parameters
    pub pixel_data: Option<Vec<u8>>,

    /// Clipboard data
    pub clipboard: Option<Vec<u8>>,
}

pub struct DesktopScreenshotRequest {
    /// The desktop to capture
    pub desktop_uuid: String,
    // TODO geometry
}

pub enum DesktopScreenshotResponse {
    Ok(Vec<u8>),
}
