use std::io::Result;

fn main() -> Result<()> {
    prost_build::compile_protos(&["src/main/proto/org/s7s/core/instance/messages.proto"], &["src/main/proto/"])?;
    Ok(())
}