//! Allows bootagents to create/restore cold snapshots while an agent is "powered off".

use sandpolis_instance::InstanceId;
use uuid::Uuid;

pub(crate) mod messages;

pub struct SnapshotLayer {}

struct SnapshotOperation {
    direction: String,
    progress: Option<f32>,
    estimation: Option<u64>,
    start_time: Option<u64>,
    end_time: Option<u64>,
    transfer_rate: u32,
    error: Option<String>,
}
