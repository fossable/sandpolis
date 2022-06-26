use std::io::Result;

fn main() -> Result<()> {
    prost_build::compile_protos(&[
        "src/main/proto/org/s7s/core/protocol/agent.proto",
        "src/main/proto/org/s7s/core/protocol/group.proto",
        "src/main/proto/org/s7s/core/protocol/network.proto",
        "src/main/proto/org/s7s/core/protocol/server.proto",
        "src/main/proto/org/s7s/core/protocol/session.proto",
        "src/main/proto/org/s7s/core/protocol/user.proto",
        "src/main/proto/org/s7s/core/protocol/stream.proto"
    ], &["src/main/proto/"])?;

    Ok(())
}