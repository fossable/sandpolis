#[data(instance)]
pub struct UsbDeviceData {
    /// null
    pub name: String,
    /// null
    pub vendor: String,
    /// null
    pub vendor_id: String,
    /// null
    pub product_id: String,
    /// Device serial number
    pub serial_number: String,
    // pub children: /profile/*/osquery/usb_device,
}
