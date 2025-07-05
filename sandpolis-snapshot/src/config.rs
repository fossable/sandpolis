use serde::{Deserialize, Serialize};
use std::path::PathBuf;

#[derive(Serialize, Deserialize, Default, Debug, Clone)]
pub struct SnapshotConfig {
    /// Path to local storage location for snapshots.
    storage: Option<PathBuf>,
}
