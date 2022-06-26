use std::io::Result;

pub mod instance {
    include!(concat!(env!("OUT_DIR"), "/core.instance.rs"));
}

fn main() -> Result<()> {
    Ok(())
}