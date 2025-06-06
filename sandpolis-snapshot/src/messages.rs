use sandpolis_core::InstanceId;
use serde::{Deserialize, Serialize};
use uuid::Uuid;

/// Create a new snapshot on a target agent.
#[derive(Serialize, Deserialize)]
pub struct CreateSnapshotRequest {
    /// The target agent's UUID
    agent_uuid: InstanceId,

    /// The target partition's UUID
    partition_uuid: Uuid,
}

/// Apply an existing snapshot on a target agent.
#[derive(Serialize, Deserialize)]
pub struct ApplySnapshotRequest {
    /// The target agent's UUID
    agent_uuid: InstanceId,

    /// The target partition's UUID
    partition_uuid: Uuid,

    // The snapshot's UUID
    snapshot_uuid: Uuid,
}

/// Create a new snapshot stream.
#[derive(Serialize, Deserialize)]
pub struct CreateSnapshotStreamRequest {
    /// The target partition's UUID
    partition_uuid: Uuid,

    /// The block size in bytes
    block_size: u32,
}

#[derive(Serialize, Deserialize)]
pub struct ApplySnapshotStreamRequest {}

/// An event containing compressed snapshot data.
#[derive(Serialize, Deserialize)]
pub struct SnapshotDataEvent {
    /// The block's offset
    offset: u64,

    /// The block's contents compressed with zlib
    data: Vec<u8>,
}

/// An event containing one or more contiguous block hashes.
#[derive(Serialize, Deserialize)]
pub struct SnapshotHashEvent {
    /// The offset of the block that the first hash corresponds
    offset: u64,

    /// A list of consecutive block hashes
    hash: Vec<u128>,
}
