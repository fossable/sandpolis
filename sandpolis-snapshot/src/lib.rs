//! Allows bootagents to create/restore cold snapshots while an agent is
//! "powered off". Regular agents cannot manage snapshots because, unless the
//! filesystem itself has snapshot support, the agent is likely to produce
//! inconsistent results (which are probably useless).
//!
//! In order to get consistent snapshots, downtime is required, so this feature
//! isn't suitable for high-availability systems.
//!
//! Snapshots are block-based which means they work with software encryption
//! schemes such as LUKS and on filesystems that don't natively support
//! snapshots.

use sandpolis_core::InstanceId;
use uuid::Uuid;

#[cfg(feature = "agent")]
pub mod agent;

#[cfg(feature = "server")]
pub mod server;

pub mod config;

pub(crate) mod messages;

pub struct SnapshotLayer {}

/// ## Create Snapshot
///
/// If there exist no previous snapshots, the agent first determines the
/// appropriate block size for the disk. The agent may take into account the
/// size of the disk or the erase-block size of an SSD, but the block size must
/// be a power of two.
///
/// If there exists a previous snapshot for the disk, the agent receives a
/// stream of block hashes. A single worker thread reads blocks from the disk
/// and compares their hashes against the block hashes retrieved from the
/// server. If the hashes do not match, the block is passed into a send queue to
/// be egressed to the server.
///
/// ## Apply Snapshot
///
/// If there exists a previous snapshot for the disk, the agent initiates a
/// stream of block hashes. A single worker thread reads blocks from the disk
/// and passes their hashes into a send queue to be egressed to the server.
///
/// Simultaneously, the agent receives a stream of block data which are placed
/// into a write queue to be written to the device.
struct SnapshotOperation {
    direction: String,
    progress: Option<f32>,
    estimation: Option<u64>,
    start_time: Option<u64>,
    end_time: Option<u64>,
    transfer_rate: u32,
    error: Option<String>,
}

pub enum SnapshotPermission {
    /// Right to create new snapshots of agent partitions
    Create,

    /// Right to apply existing snapshots to agent partitions
    Apply,

    /// Right to view metadata about snapshots on a server
    List,
}
