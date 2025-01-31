//! Allows bootagents to create/restore cold snapshots while an agent is "powered off".

use sandpolis_instance::InstanceId;
use uuid::Uuid;

struct SnapshotOperation {
    direction: String,
    progress: Option<f32>,
    estimation: Option<u64>,
    start_time: Option<u64>,
    end_time: Option<u64>,
    transfer_rate: u32,
    error: Option<String>,
}

/// Create a new snapshot on a target agent.
pub struct CreateSnapshotRequest {
    /// The target agent's UUID
    agent_uuid: InstanceId,

    /// The target partition's UUID
    partition_uuid: Uuid,
}

/// Apply an existing snapshot on a target agent.
pub struct ApplySnapshotRequest {
    /// The target agent's UUID
    agent_uuid: InstanceId,

    /// The target partition's UUID
    partition_uuid: Uuid,

    // The snapshot's UUID
    snapshot_uuid: Uuid,
}

/// Create a new snapshot stream.
pub struct CreateSnapshotStreamRequest {
    /// The target partition's UUID
    partition_uuid: Uuid,

    /// The block size in bytes
    block_size: u32,
}

pub struct ApplySnapshotStreamRequest {}

/// An event containing compressed snapshot data.
pub struct SnapshotDataEvent {
    /// The block's offset
    offset: u64,

    /// The block's contents compressed with zlib
    data: Vec<u8>,
}

/// An event containing one or more contiguous block hashes.
pub struct SnapshotHashEvent {
    /// The offset of the block that the first hash corresponds
    offset: u64,

    /// A list of consecutive block hashes
    hash: Vec<u128>,
}
