use serde::{Deserialize, Serialize};

#[derive(Clone, Serialize, Deserialize)]
pub struct Tunnel {

	/// The instance ID that hosts the listener
	pub listener_iid: String,

	/// The listening port
	pub listener_port: u16,

	pub repeater_iid: Vec<String>,

	pub terminator_iid: String,

	pub target_ip: String,

	pub target_port: u16,

	pub target_protocol: String,
}

impl Tunnel {

	pub fn listen(&self) {

	}

	pub fn terminate(&self) {
		
	}
}