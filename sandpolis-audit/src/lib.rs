use native_db::*;
use native_model::{Model, native_model};
use sandpolis_core::InstanceId;
use sandpolis_database::DbTimestamp;
use serde::{Deserialize, Serialize};

#[derive(Serialize, Deserialize, PartialEq, Debug)]
#[native_model(id = 2, version = 1)]
#[native_db]
pub struct SignatureDetectionData {
    #[primary_key]
    pub id: u32,

    #[secondary_key]
    pub instance_id: InstanceId,

    #[secondary_key]
    pub timestamp: DbTimestamp,
}

#[derive(Serialize, Deserialize, PartialEq, Debug)]
#[native_model(id = 4, version = 1)]
#[native_db]
pub struct EventDetectionData {
    #[primary_key]
    pub id: u32,

    #[secondary_key]
    pub instance_id: InstanceId,

    #[secondary_key]
    pub timestamp: DbTimestamp,
}

/// Indicates the degree to which the user is currently participating in their
/// computing experience. We can vary auditing behavior as a result.
pub enum UserPresence {
    /// The user is actively using the machine. They might be making various
    /// changes and system load will vary. We should reduce auditing
    /// activity to avoid performance impacts.
    Active,

    /// The user has not interacted with the system in a sufficiently long time,
    /// but they could come back soon. We can increase auditing
    /// activity since the user won't notice.
    Idle,

    /// The user is intentionally "away" and thus any user activity is
    /// automatically considered suspicious.
    Away,
}

pub enum DeviceClass {
    Workstation,
    Server,
    Embedded,
}
