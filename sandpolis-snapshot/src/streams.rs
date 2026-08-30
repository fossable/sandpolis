//! Wire types for the snapshot streams.
//!
//! Three stream types exist:
//!
//! - `SnapshotMgmtStream` (client -> server): create/apply/delete requests and
//!   their terminal outcomes. The responder lives in [`crate::server`], the
//!   requester in [`crate::client`].
//! - `SnapshotCreateStream` (server -> agent): the block exchange for a
//!   capture. The agent streams hashes up; the server answers with the offsets
//!   it needs and the agent uploads those blocks.
//! - `SnapshotApplyStream` (server -> agent): the block exchange for a
//!   restore. The agent streams hashes up; the server answers with the blocks
//!   that differ and the agent writes them to the partition.

use sandpolis_instance::InstanceId;
use serde::{Deserialize, Serialize};

/// Ask the server to run a snapshot operation on one of its attached agents.
#[derive(Serialize, Deserialize)]
pub enum SnapshotMgmtRequest {
    /// Capture a new snapshot of a partition
    Create {
        agent: InstanceId,
        partition_uuid: String,
        label: Option<String>,
    },
    /// Restore an existing snapshot onto a partition
    Apply {
        agent: InstanceId,
        partition_uuid: String,
        snapshot_uuid: String,
    },
    /// Delete a stored snapshot (leaf layers only)
    Delete {
        agent: InstanceId,
        partition_uuid: String,
        snapshot_uuid: String,
    },
}

#[derive(Serialize, Deserialize, Debug)]
pub enum SnapshotMgmtResponse {
    /// The operation was accepted and is running
    Started,
    /// A create/apply operation finished; `snapshot_uuid` names the new layer
    /// on create
    Finished {
        snapshot_uuid: Option<String>,
        /// Block bytes transferred (before compression)
        bytes: u64,
    },
    Deleted,
    Failed(String),
}

/// Server -> agent messages on a create stream.
#[derive(Serialize, Deserialize)]
pub enum SnapshotCreateRequest {
    /// Begin scanning the partition
    Start {
        partition_uuid: String,
        block_size: u64,
    },
    /// Upload the blocks at these offsets; they differ from the previous
    /// snapshot
    Need { offsets: Vec<u64> },
    /// Every hash batch has been compared; no further `Need` will follow
    NeedDone,
}

/// Agent -> server messages on a create stream.
#[derive(Serialize, Deserialize)]
pub enum SnapshotCreateResponse {
    /// Partition size as measured by the agent, sent once before any hashes
    Meta {
        size: u64,
    },
    /// blake3 hashes of consecutive blocks starting at `offset`
    Hashes {
        offset: u64,
        hashes: Vec<[u8; 32]>,
    },
    /// Every block has been hashed
    HashesDone,
    /// A requested block, zstd-compressed
    Block {
        offset: u64,
        data: Vec<u8>,
    },
    /// Every requested block has been uploaded
    Done,
    Failed(String),
}

/// Server -> agent messages on an apply stream.
#[derive(Serialize, Deserialize)]
pub enum SnapshotApplyRequest {
    /// Begin restoring the partition; `size` is the snapshot's captured size
    Start {
        partition_uuid: String,
        block_size: u64,
        size: u64,
    },
    /// A block that differs from the snapshot, zstd-compressed
    Block { offset: u64, data: Vec<u8> },
    /// Every differing block has been sent
    Done,
}

/// Agent -> server messages on an apply stream.
#[derive(Serialize, Deserialize)]
pub enum SnapshotApplyResponse {
    /// blake3 hashes of consecutive blocks starting at `offset`
    Hashes {
        offset: u64,
        hashes: Vec<[u8; 32]>,
    },
    /// Every block has been hashed
    HashesDone,
    /// All received blocks are on disk
    Applied,
    Failed(String),
}
