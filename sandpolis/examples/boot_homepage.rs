//! Runs the boot-mode homepage against mocked state, so the UI can be
//! reviewed without a server or efivars:
//!
//! ```sh
//! cargo run -p sandpolis --features agent --example boot_homepage
//! ```
//!
//! A mock server "connects" after a few seconds. Set `BOOT_MOCK_HOLD=1` to
//! have it place a boot hold, run a mock snapshot operation through the
//! block-grid display, and then release the hold (the "reboot" is logged).

use sandpolis::agent::boot::run_boot_ui;
use sandpolis_agent::bootagent::BootAgentState;
use sandpolis_agent::uefi::BootEntry;
use std::sync::Arc;
use std::sync::atomic::Ordering;
use std::time::Duration;

fn main() -> anyhow::Result<()> {
    tracing_subscriber::fmt().init();

    let state = Arc::new(BootAgentState::default());
    let hold = std::env::var("BOOT_MOCK_HOLD").is_ok();

    let mock = state.clone();
    std::thread::spawn(move || {
        std::thread::sleep(Duration::from_secs(3));
        *mock.server.lock().unwrap() = Some("172.16.10.1:8768".into());

        if hold {
            std::thread::sleep(Duration::from_secs(1));
            mock.hold.store(true, Ordering::Relaxed);

            // A snapshot operation arrives shortly after the hold, and the
            // release shortly after it completes
            std::thread::sleep(Duration::from_secs(2));
            mock_snapshot();
            std::thread::sleep(Duration::from_secs(5));
            mock.release.store(true, Ordering::Relaxed);
        }
    });

    let entries = vec![
        BootEntry {
            number: 1,
            description: "Arch Linux".into(),
        },
        BootEntry {
            number: 2,
            description: "Windows Boot Manager".into(),
        },
        BootEntry {
            number: 4,
            description: "UEFI Shell".into(),
        },
    ];

    run_boot_ui(state, entries, |entry| match entry {
        Some(entry) => println!(
            "(mock) chainloading Boot{:04X}: {}",
            entry.number, entry.description
        ),
        None => println!("(mock) rebooting"),
    })
}

/// Drive a mocked snapshot capture through the same publish/clear path the
/// real agent workers use. Blocks until it completes.
fn mock_snapshot() {
    use sandpolis_snapshot::boot_snapshot::{self, BlockState, SnapshotProgress};
    use sandpolis_snapshot::{SNAPSHOT_BLOCK_SIZE, SnapshotDirection};

    let size = 4 * (1 << 30); // 4 GiB partition
    let progress = Arc::new(SnapshotProgress::new(
        SnapshotDirection::Create,
        "nvme0n1p2".into(),
        size,
        SNAPSHOT_BLOCK_SIZE,
    ));
    boot_snapshot::publish(progress.clone());

    // The scan marks a fifth of the blocks dirty; uploads follow behind
    let total = progress.blocks.len();
    let mut needed = Vec::new();
    for i in 0..total {
        progress.set(i, BlockState::Hashing);
        if i % 5 == 0 {
            progress.set(i, BlockState::Needed);
            needed.push(i);
        } else {
            progress.set(i, BlockState::Clean);
        }
        std::thread::sleep(Duration::from_millis(1));
    }
    for i in needed {
        progress.set(i, BlockState::Transferring);
        // Pretend zstd got a 40% ratio
        progress.add_bytes(SNAPSHOT_BLOCK_SIZE * 2 / 5);
        progress.set(i, BlockState::Done);
        std::thread::sleep(Duration::from_millis(2));
    }
    progress.finish();
    boot_snapshot::clear();
}
