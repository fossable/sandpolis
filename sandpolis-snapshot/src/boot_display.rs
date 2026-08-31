//! Fullscreen "grid of blocks" progress display for snapshot operations
//! running in the UKI boot environment.
//!
//! Rendering uses Slint's software renderer. On a desktop (and in the
//! `boot_display` example) the winit backend provides a window; in the UKI
//! there is no compositor, so the linuxkms backend draws straight to
//! `/dev/dri/card0` (select it with `SLINT_BACKEND=linuxkms-noseat`).
//!
//! The snapshot worker publishes per-block state into a shared
//! [`SnapshotProgress`] and the display samples it ~10 times a second. Since a
//! partition usually has far more blocks than the screen has cells, each cell
//! aggregates a contiguous block range and shows the most urgent state in it.

use crate::SnapshotDirection;
use anyhow::Result;
use slint::{ComponentHandle, Image, Rgb8Pixel, SharedPixelBuffer, Timer, TimerMode};
use std::sync::Arc;
use std::sync::atomic::{AtomicBool, AtomicU8, AtomicU64, Ordering};
use std::time::{Duration, Instant};

slint::include_modules!();

/// Side length of one grid cell in pixels.
const CELL_SIZE: u32 = 10;

/// Distance between cell origins in pixels (cell + gap).
const CELL_PITCH: u32 = 12;

/// What the operation currently knows about one block. Stored as a `u8` so the
/// worker can update it with a single relaxed atomic store.
#[derive(Clone, Copy, PartialEq, Eq, Debug, Default)]
#[repr(u8)]
pub enum BlockState {
    /// Not yet reached by the scan
    #[default]
    Pending = 0,
    /// Matched the server's copy (or was never requested), nothing to send
    Clean = 1,
    /// Finished transferring
    Done = 2,
    /// The server asked for this block; upload not started yet
    Needed = 3,
    /// Being read and hashed right now
    Hashing = 4,
    /// Block data is on the wire
    Transferring = 5,
    /// The operation gave up on this block
    Failed = 6,
}

impl BlockState {
    fn from_u8(value: u8) -> Self {
        match value {
            1 => Self::Clean,
            2 => Self::Done,
            3 => Self::Needed,
            4 => Self::Hashing,
            5 => Self::Transferring,
            6 => Self::Failed,
            _ => Self::Pending,
        }
    }

    /// Whether the block needs no further work.
    fn terminal(self) -> bool {
        matches!(self, Self::Clean | Self::Done | Self::Failed)
    }

    fn color(self) -> Rgb8Pixel {
        match self {
            // #4a4a4a at ~40% over the #333333 background
            Self::Pending => Rgb8Pixel::new(0x3c, 0x3c, 0x3c),
            Self::Clean => Rgb8Pixel::new(0x3a, 0x5a, 0x8a),
            Self::Done => Rgb8Pixel::new(0x5a, 0x8a, 0x5a),
            Self::Needed => Rgb8Pixel::new(0x7a, 0x68, 0x26),
            Self::Hashing | Self::Transferring => Rgb8Pixel::new(0xc8, 0xab, 0x37),
            Self::Failed => Rgb8Pixel::new(0xaa, 0x33, 0x33),
        }
    }
}

/// Shared state between a snapshot worker and the display. All updates are
/// relaxed atomics so publishing progress costs the worker almost nothing.
pub struct SnapshotProgress {
    pub direction: SnapshotDirection,
    /// Human-readable name of the partition being captured or restored
    pub label: String,
    /// Transfer block size in bytes
    pub block_size: u64,
    /// One state per block of the partition
    pub blocks: Vec<AtomicU8>,
    /// Block bytes moved so far (after compression)
    pub bytes_transferred: AtomicU64,
    pub done: AtomicBool,
    error: std::sync::Mutex<Option<String>>,
}

impl SnapshotProgress {
    pub fn new(direction: SnapshotDirection, label: String, size: u64, block_size: u64) -> Self {
        let total_blocks = size.div_ceil(block_size).max(1) as usize;
        Self {
            direction,
            label,
            block_size,
            blocks: std::iter::repeat_with(AtomicU8::default)
                .take(total_blocks)
                .collect(),
            bytes_transferred: AtomicU64::new(0),
            done: AtomicBool::new(false),
            error: std::sync::Mutex::new(None),
        }
    }

    /// Update the block at a given index.
    pub fn set(&self, index: usize, state: BlockState) {
        if let Some(block) = self.blocks.get(index) {
            block.store(state as u8, Ordering::Relaxed);
        }
    }

    /// Update the block containing a given byte offset.
    pub fn set_offset(&self, offset: u64, state: BlockState) {
        self.set((offset / self.block_size) as usize, state);
    }

    pub fn get(&self, index: usize) -> BlockState {
        self.blocks
            .get(index)
            .map(|b| BlockState::from_u8(b.load(Ordering::Relaxed)))
            .unwrap_or_default()
    }

    pub fn add_bytes(&self, bytes: u64) {
        self.bytes_transferred.fetch_add(bytes, Ordering::Relaxed);
    }

    pub fn finish(&self) {
        self.done.store(true, Ordering::Relaxed);
    }

    pub fn fail(&self, message: impl Into<String>) {
        *self.error.lock().unwrap() = Some(message.into());
        self.done.store(true, Ordering::Relaxed);
    }

    pub fn error(&self) -> Option<String> {
        self.error.lock().unwrap().clone()
    }

    /// Fraction of blocks that need no further work.
    pub fn fraction_complete(&self) -> f32 {
        let terminal = self
            .blocks
            .iter()
            .filter(|b| BlockState::from_u8(b.load(Ordering::Relaxed)).terminal())
            .count();
        terminal as f32 / self.blocks.len() as f32
    }

    fn describe(&self) -> String {
        match self.direction {
            SnapshotDirection::Create => format!("Creating snapshot of {}", self.label),
            SnapshotDirection::Apply => format!("Applying snapshot to {}", self.label),
        }
    }
}

/// Rasterize the block grid into a pixel buffer sized for the given area.
fn render_grid(progress: &SnapshotProgress, avail_width: u32, avail_height: u32) -> Image {
    let total = progress.blocks.len();
    let cols = (avail_width / CELL_PITCH).max(1) as usize;
    let rows = (avail_height / CELL_PITCH).max(1) as usize;
    let cells = cols * rows;

    let width = (cols as u32 * CELL_PITCH) - (CELL_PITCH - CELL_SIZE);
    let height = (rows as u32 * CELL_PITCH) - (CELL_PITCH - CELL_SIZE);
    let mut buffer = SharedPixelBuffer::<Rgb8Pixel>::new(width, height);
    let stride = width as usize;
    let pixels = buffer.make_mut_slice();
    pixels.fill(Rgb8Pixel::new(0x33, 0x33, 0x33));

    for cell in 0..cells {
        // Aggregate the cell's block range, showing its most urgent state
        let start = cell * total / cells;
        let end = (((cell + 1) * total / cells).max(start + 1)).min(total);
        let state = (start..end)
            .map(|i| progress.get(i))
            .max_by_key(|s| *s as u8)
            .unwrap_or_default();

        let color = state.color();
        let x0 = (cell % cols) as u32 * CELL_PITCH;
        let y0 = (cell / cols) as u32 * CELL_PITCH;
        for y in y0..y0 + CELL_SIZE {
            let row = &mut pixels[y as usize * stride + x0 as usize..][..CELL_SIZE as usize];
            row.fill(color);
        }
    }

    Image::from_rgb8(buffer)
}

fn format_bytes(bytes: u64) -> String {
    match bytes {
        b if b >= 1 << 30 => format!("{:.1} GiB", b as f64 / (1u64 << 30) as f64),
        b if b >= 1 << 20 => format!("{:.1} MiB", b as f64 / (1u64 << 20) as f64),
        b => format!("{:.1} KiB", b as f64 / (1u64 << 10) as f64),
    }
}

/// Run the display until the window closes (winit) or forever (linuxkms). This
/// owns the calling thread; the snapshot operation drives `progress` from
/// other threads.
pub fn run_boot_display(progress: Arc<SnapshotProgress>) -> Result<()> {
    let ui = BootDisplay::new()?;
    ui.set_operation(progress.describe().into());

    let ui_weak = ui.as_weak();
    let mut last_sample = (Instant::now(), 0u64);
    let mut rate = 0.0f64;
    let mut finished = false;
    let timer = Timer::default();
    timer.start(TimerMode::Repeated, Duration::from_millis(100), move || {
        let Some(ui) = ui_weak.upgrade() else { return };
        if finished {
            return;
        }

        ui.set_grid(render_grid(
            &progress,
            ui.get_grid_width() as u32,
            ui.get_grid_height() as u32,
        ));

        let bytes = progress.bytes_transferred.load(Ordering::Relaxed);
        let elapsed = last_sample.0.elapsed();
        if elapsed >= Duration::from_secs(1) {
            rate = (bytes - last_sample.1) as f64 / elapsed.as_secs_f64();
            last_sample = (Instant::now(), bytes);
        }

        let fraction = progress.fraction_complete();
        ui.set_progress(fraction);

        if progress.done.load(Ordering::Relaxed) {
            finished = true;
            if let Some(error) = progress.error() {
                ui.set_failed(true);
                ui.set_stats(error.into());
            } else {
                ui.set_progress(1.0);
                ui.set_stats(format!("Complete · {} sent", format_bytes(bytes)).into());
            }
        } else {
            ui.set_stats(
                format!(
                    "{:.0}% · {} sent · {}/s",
                    fraction * 100.0,
                    format_bytes(bytes),
                    format_bytes(rate as u64),
                )
                .into(),
            );
        }
    });

    ui.run()?;
    Ok(())
}
