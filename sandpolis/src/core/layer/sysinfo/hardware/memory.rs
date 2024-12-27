/// Handle, or instance number, associated with the structure in SMBIOS
pub handle: java.lang.String,
/// The memory array that the device is attached to
pub array_handle: java.lang.String,
/// Implementation form factor for this memory device
pub form_factor: java.lang.String,
/// Total width, in bits, of this memory device, including any check or error-correction bits
pub total_width: java.lang.Integer,
/// Data width, in bits, of this memory device
pub data_width: java.lang.Integer,
/// Size of memory device in bytes
pub size: java.lang.Integer,
/// Identifies if memory device is one of a set of devices. A value of 0 indicates no set affiliation.
pub set: java.lang.Integer,
/// String number of the string that identifies the physically-labeled socket or board position where the memory device is located
pub device_location: java.lang.String,
/// String number of the string that identifies the physically-labeled bank where the memory device is located
pub bank_location: java.lang.String,
/// Type of memory used
pub memory_type: java.lang.String,
/// Additional details for memory device
pub memory_type_details: java.lang.String,
/// Max speed of memory device in megatransfers per second (MT/s)
pub max_speed: java.lang.Integer,
/// Configured speed of memory device in megatransfers per second (MT/s)
pub configured_clock_speed: java.lang.Integer,
/// Manufacturer ID string
pub manufacturer: java.lang.String,
/// Serial number of memory device
pub serial_number: java.lang.String,
/// Manufacturer specific asset tag of memory device
pub asset_tag: java.lang.String,
/// Manufacturer specific serial number of memory device
pub part_number: java.lang.String,
/// Minimum operating voltage of device in millivolts
pub min_voltage: java.lang.Integer,
/// Maximum operating voltage of device in millivolts
pub max_voltage: java.lang.Integer,
/// Configured operating voltage of device in millivolts
pub configured_voltage: java.lang.Integer,
