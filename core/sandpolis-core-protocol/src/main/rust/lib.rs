
pub mod core {
	pub mod protocol {
	    include!(concat!(env!("OUT_DIR"), "/core.protocol.rs"));
	}
}