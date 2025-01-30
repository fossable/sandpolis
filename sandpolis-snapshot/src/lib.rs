//! Allows bootagents to create/restore cold snapshots while an agent is "powered off".

struct SnapshotOperation {
    direction: String,
    progress: Option<f32>,
    estimation: Option<u64>,
    start_time: Option<u64>,
    end_time: Option<u64>,
    transfer_rate: u32,
    error: Option<String>,
}

// Create a new snapshot on a target agent.
//
// Sources      : client
// Destinations : server
//
message RQ_CreateSnapshot {

    // The target agent's UUID
    string agent_uuid = 1;

    // The target partition's UUID
    string partition_uuid = 2;
}

// Apply an existing snapshot on a target agent.
//
// Sources      : client
// Destinations : server
//
message RQ_ApplySnapshot {

    // The target agent's UUID
    string agent_uuid = 1;

    // The target partition's UUID
    string partition_uuid = 2;

    // The snapshot's UUID
    string snapshot_uuid = 3;
}

// Create a new snapshot stream.
//
// Sources      : server, agent
// Destinations : server, agent
//
message RQ_SnapshotStream {

    enum SnapshotOperation {
        SNAPSHOT_CREATE = 0;
        SNAPSHOT_APPLY = 1;
    }

    // The stream's ID
    int32 stream_id = 1;

    // The snapshot operation type
    SnapshotOperation operation = 2;

    // The target partition uuid
    string partition_uuid = 3;

    // The block size in bytes
    int32 block_size = 4;
}

// An event containing compressed snapshot data.
//
// Sources      : server, agent
// Destinations : server, agent
//
message EV_SnapshotDataBlock {

    // The block's offset
    int64 offset = 1;

    // The block's contents compressed with zlib
    bytes data = 2;
}

// An event containing one or more contiguous block hashes.
//
// Sources      : server, agent
// Destinations : server, agent
//
message EV_SnapshotHashBlock {

    // The offset of the block that the first hash corresponds
    int64 offset = 1;

    // A list of consecutive block hashes
    repeated bytes hash = 2;
}
