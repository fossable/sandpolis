//! Runs the boot-mode snapshot display against a mocked snapshot operation, so
//! the UI can be reviewed without a server or agent:
//!
//! ```sh
//! cargo run -p sandpolis-snapshot --features agent --example boot_display
//! ```
//!
//! Set `SNAPSHOT_MOCK_FAILURES=1` to sprinkle in failed blocks.

use sandpolis_snapshot::boot_display::{BlockState, SnapshotProgress, run_boot_display};
use sandpolis_snapshot::{SNAPSHOT_BLOCK_SIZE, SnapshotDirection};
use std::sync::Arc;
use std::time::Duration;

/// Cheap deterministic hash used to pick which mock blocks are "dirty".
fn mix(i: u64) -> u64 {
    i.wrapping_mul(0x9E3779B97F4A7C15).rotate_left(31)
}

fn main() -> anyhow::Result<()> {
    let size = 8 * (1 << 30); // 8 GiB partition
    let progress = Arc::new(SnapshotProgress::new(
        SnapshotDirection::Create,
        "nvme0n1p2 (8.0 GiB)".into(),
        size,
        SNAPSHOT_BLOCK_SIZE,
    ));
    let total = progress.blocks.len();
    let failures = std::env::var("SNAPSHOT_MOCK_FAILURES").is_ok();

    let mock = progress.clone();
    std::thread::spawn(move || {
        let mut scan = 0usize;
        let mut needed: Vec<usize> = Vec::new();
        let mut transfer = 0usize;
        let mut tick = 0u64;

        loop {
            // The hash scan sweeps the whole partition; the "server" asks for
            // roughly a fifth of the blocks
            for _ in 0..4 {
                if scan < total {
                    mock.set(scan, BlockState::Hashing);
                    if mix(scan as u64) % 5 == 0 {
                        mock.set(scan, BlockState::Needed);
                        needed.push(scan);
                    } else {
                        mock.set(scan, BlockState::Clean);
                    }
                    scan += 1;
                }
            }

            // Uploads trail behind the scan at a slower pace
            if transfer < needed.len() {
                let block = needed[transfer];
                mock.set(block, BlockState::Transferring);
                if tick % 3 == 0 {
                    if failures && mix(block as u64) % 97 == 0 {
                        mock.set(block, BlockState::Failed);
                    } else {
                        mock.set(block, BlockState::Done);
                        // Pretend zstd got a 40% ratio
                        mock.add_bytes(SNAPSHOT_BLOCK_SIZE * 2 / 5);
                    }
                    transfer += 1;
                }
            }

            if scan >= total && transfer >= needed.len() {
                mock.finish();
                break;
            }
            tick += 1;
            std::thread::sleep(Duration::from_millis(5));
        }
    });

    run_boot_display(progress)
}
