pub struct Battery {
	/// Manufacturer's name
	pub manufacturer: Option<String>,

	/// The date the battery was manufactured UNIX Epoch
	pub manufacture_date: Option<u64>,

	/// Model number
	pub model: Option<String>,

	/// Serial number
	pub serial_number: Option<String>,

	/// Number of charge/discharge cycles
	pub cycle_count: Option<u64>,

	/// Whether the battery is currently being changed by a power source
	pub charging: Option<bool>,

	/// Whether the battery is completely charged
	pub charged: Option<bool>,
}

{
	"collection": true,
	"attributes": [
	{
		"name": "charged",
		"type": "java.lang.Boolean",
		"description": "Whether the battery is completely charged",
		"osquery": "battery.charged"
	},
	{
		"name": "designed_capacity",
		"type": "java.lang.Integer",
		"description": "The battery's designed capacity in mAh",
		"osquery": "battery.designed_capacity"
	},
	{
		"name": "max_capacity",
		"type": "java.lang.Integer",
		"description": "The battery's actual capacity when it is fully charged in mAh",
		"osquery": "battery.max_capacity"
	},
	{
		"name": "current_capacity",
		"type": "java.lang.Integer",
		"description": "The battery's current charged capacity in mAh",
		"osquery": "battery.current_capacity"
	},
	{
		"name": "percent_remaining",
		"type": "java.lang.Integer",
		"description": "The percentage of battery remaining before it is drained",
		"osquery": "battery.percent_remaining"
	},
	{
		"name": "amperage",
		"type": "java.lang.Integer",
		"description": "The battery's current amperage in mA",
		"osquery": "battery.amperage"
	},
	{
		"name": "voltage",
		"type": "java.lang.Integer",
		"description": "The battery's current voltage in mV",
		"osquery": "battery.voltage"
	}
]}
