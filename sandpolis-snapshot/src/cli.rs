use anyhow::Result;
use clap::Parser;
use std::path::PathBuf;

#[derive(Parser, Debug, Clone)]
pub struct SnapshotCommandLine {
    /// Path to local storage location for snapshots.
    #[clap(long)]
    snapshot_path: Option<PathBuf>,
}
