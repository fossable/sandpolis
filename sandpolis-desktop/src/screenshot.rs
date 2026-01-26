pub struct DesktopScreenshotRequest {
    /// The desktop to capture
    pub desktop_uuid: String,
    // TODO geometry
}

pub enum DesktopScreenshotResponse {
    Ok(Vec<u8>),
}
