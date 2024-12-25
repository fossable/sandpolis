
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

message Desktop {

    // The desktop name
    string name = 1;

    // The desktop width in pixels
    int32 width = 2;

    // The desktop height in pixels
    int32 height = 3;
}

// Response containing all available desktops
message RS_DesktopList {


    repeated Desktop desktop = 1;
}

message RQ_DesktopStream {


    // The requested stream ID
    int32 stream_id = 1;

    // The desktop to capture
    string desktop_uuid = 2;

    // The screen scale factor
    double scale_factor = 3;
}

enum RS_DesktopStream {
    DESKTOP_STREAM_OK = 0;
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

pub struct DesktopScreenshotResponse {
    pub data: Vec<u8>,
}
