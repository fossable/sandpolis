//! Agent side of the block streams.
//!
//! Both responders follow the shell session pattern: `on_message` never touches
//! the disk. The `Start` request spawns a worker task that owns the device file
//! and drives the whole exchange; later messages are forwarded into it over a
//! channel. Blocking in `on_message` would stall the connection's dispatch
//! loop, since responder handlers run inline on the socket's receive path.

use crate::streams::*;
use crate::{HASHES_PER_MESSAGE, SNAPSHOT_BLOCK_SIZE};
use anyhow::{Result, bail};
use sandpolis_instance::network::StreamResponder;
use sandpolis_macros::Stream;
use std::io::SeekFrom;
use std::path::{Path, PathBuf};
use tokio::fs::{File, OpenOptions};
use tokio::io::{AsyncReadExt, AsyncSeekExt, AsyncWriteExt};
use tokio::sync::Mutex;
use tokio::sync::mpsc::{Sender, UnboundedReceiver, UnboundedSender, unbounded_channel};
use tracing::{debug, warn};

/// zstd level for block uploads: fast enough to keep up with disk reads while
/// still collapsing typical filesystem content well.
const WIRE_COMPRESSION_LEVEL: i32 = 3;

/// Resolve a partition UUID to its device node through the udev symlinks.
pub fn resolve_partition(partition_uuid: &str) -> Result<PathBuf> {
    // The uuid becomes a path component
    if !crate::valid_uuid(partition_uuid) {
        bail!("Invalid partition UUID");
    }
    let path = Path::new("/dev/disk/by-partuuid").join(partition_uuid);
    Ok(std::fs::canonicalize(&path)?)
}

/// Size of a partition (or any seekable file) in bytes.
async fn device_size(file: &mut File) -> Result<u64> {
    let size = file.seek(SeekFrom::End(0)).await?;
    file.seek(SeekFrom::Start(0)).await?;
    Ok(size)
}

/// Read the block at `offset`, which is shorter than `block_size` only at the
/// end of the device.
async fn read_block(file: &mut File, offset: u64, block_size: u64, size: u64) -> Result<Vec<u8>> {
    let len = block_size.min(size - offset) as usize;
    let mut block = vec![0u8; len];
    file.seek(SeekFrom::Start(offset)).await?;
    file.read_exact(&mut block).await?;
    Ok(block)
}

/// Stream that captures a snapshot: hashes every block for the server and
/// uploads the ones it asks for.
#[derive(Stream, Default)]
pub struct SnapshotCreateStreamResponder {
    // Unbounded on purpose: if forwarding into the worker could block, the
    // connection's dispatch loop would stop reading the socket while the worker
    // is itself blocked sending to the server — a bidirectional-pipe deadlock.
    // The backlog stays small because the server only requests blocks in
    // response to hashes the worker already sent.
    worker: Mutex<Option<UnboundedSender<SnapshotCreateRequest>>>,
}

impl StreamResponder for SnapshotCreateStreamResponder {
    type In = SnapshotCreateRequest;
    type Out = SnapshotCreateResponse;

    async fn on_message(&self, request: Self::In, sender: Sender<Self::Out>) -> Result<()> {
        match request {
            SnapshotCreateRequest::Start {
                partition_uuid,
                block_size,
            } => {
                let (tx, rx) = unbounded_channel();
                *self.worker.lock().await = Some(tx);
                tokio::spawn(async move {
                    if let Err(e) = create_worker(&partition_uuid, block_size, rx, &sender).await {
                        warn!(error = %e, "Snapshot capture failed");
                        let _ = sender
                            .send(SnapshotCreateResponse::Failed(e.to_string()))
                            .await;
                    }
                });
            }
            other => {
                if let Some(worker) = self.worker.lock().await.as_ref() {
                    let _ = worker.send(other);
                }
            }
        }
        Ok(())
    }
}

async fn create_worker(
    partition_uuid: &str,
    block_size: u64,
    mut requests: UnboundedReceiver<SnapshotCreateRequest>,
    sender: &Sender<SnapshotCreateResponse>,
) -> Result<()> {
    if block_size == 0 || !block_size.is_power_of_two() {
        bail!("Block size must be a power of two");
    }

    let path = resolve_partition(partition_uuid)?;
    let mut file = File::open(&path).await?;
    let size = device_size(&mut file).await?;
    debug!(?path, size, "Capturing partition snapshot");

    sender.send(SnapshotCreateResponse::Meta { size }).await?;

    // Hash every block, flushing batches as they fill. Upload requests arrive
    // interleaved with the scan (the server compares batches as they land), so
    // they're drained between blocks rather than only after the scan.
    let mut batch_start = 0u64;
    let mut hashes: Vec<[u8; 32]> = Vec::with_capacity(HASHES_PER_MESSAGE);
    let mut offset = 0u64;
    while offset < size {
        while let Ok(request) = requests.try_recv() {
            handle_upload(request, &mut file, block_size, size, sender).await?;
        }

        let block = read_block(&mut file, offset, block_size, size).await?;
        hashes.push(blake3::hash(&block).into());
        offset += block_size;

        if hashes.len() >= HASHES_PER_MESSAGE || offset >= size {
            sender
                .send(SnapshotCreateResponse::Hashes {
                    offset: batch_start,
                    hashes: std::mem::take(&mut hashes),
                })
                .await?;
            batch_start = offset;
        }
    }
    sender.send(SnapshotCreateResponse::HashesDone).await?;

    // Serve the remaining upload requests until the server has compared
    // everything.
    while let Some(request) = requests.recv().await {
        if !handle_upload(request, &mut file, block_size, size, sender).await? {
            break;
        }
    }
    sender.send(SnapshotCreateResponse::Done).await?;
    debug!(?path, "Partition scan complete");
    Ok(())
}

/// Serve one server request during a capture. Returns false once the server
/// has promised no further `Need`s.
async fn handle_upload(
    request: SnapshotCreateRequest,
    file: &mut File,
    block_size: u64,
    size: u64,
    sender: &Sender<SnapshotCreateResponse>,
) -> Result<bool> {
    match request {
        SnapshotCreateRequest::Need { offsets } => {
            for offset in offsets {
                if offset >= size {
                    bail!("Requested block is out of range");
                }
                let block = read_block(file, offset, block_size, size).await?;
                sender
                    .send(SnapshotCreateResponse::Block {
                        offset,
                        data: zstd::bulk::compress(&block, WIRE_COMPRESSION_LEVEL)?,
                    })
                    .await?;
            }
            Ok(true)
        }
        SnapshotCreateRequest::NeedDone => Ok(false),
        SnapshotCreateRequest::Start { .. } => bail!("Stream already started"),
    }
}

/// Stream that restores a snapshot: hashes every block for the server and
/// writes back the ones that differ.
#[derive(Stream, Default)]
pub struct SnapshotApplyStreamResponder {
    // Unbounded for the same deadlock-avoidance reason as the create stream;
    // restored blocks are only sent in response to hashes, so the drain between
    // scanned blocks keeps the backlog to what the network delivered meanwhile.
    worker: Mutex<Option<UnboundedSender<SnapshotApplyRequest>>>,
}

impl StreamResponder for SnapshotApplyStreamResponder {
    type In = SnapshotApplyRequest;
    type Out = SnapshotApplyResponse;

    async fn on_message(&self, request: Self::In, sender: Sender<Self::Out>) -> Result<()> {
        match request {
            SnapshotApplyRequest::Start {
                partition_uuid,
                block_size,
                size,
            } => {
                let (tx, rx) = unbounded_channel();
                *self.worker.lock().await = Some(tx);
                tokio::spawn(async move {
                    if let Err(e) =
                        apply_worker(&partition_uuid, block_size, size, rx, &sender).await
                    {
                        warn!(error = %e, "Snapshot restore failed");
                        let _ = sender
                            .send(SnapshotApplyResponse::Failed(e.to_string()))
                            .await;
                    }
                });
            }
            other => {
                if let Some(worker) = self.worker.lock().await.as_ref() {
                    let _ = worker.send(other);
                }
            }
        }
        Ok(())
    }
}

async fn apply_worker(
    partition_uuid: &str,
    block_size: u64,
    size: u64,
    mut requests: UnboundedReceiver<SnapshotApplyRequest>,
    sender: &Sender<SnapshotApplyResponse>,
) -> Result<()> {
    if block_size == 0 || !block_size.is_power_of_two() {
        bail!("Block size must be a power of two");
    }

    let path = resolve_partition(partition_uuid)?;
    let mut file = OpenOptions::new()
        .read(true)
        .write(true)
        .open(&path)
        .await?;
    let local_size = device_size(&mut file).await?;
    if local_size != size {
        bail!("Partition size changed since the snapshot was taken ({local_size} != {size})");
    }
    debug!(?path, size, "Restoring partition snapshot");

    // Hash every block so the server can diff against the snapshot. Differing
    // blocks start arriving while the scan runs; a block is only ever sent
    // after its hash was received, so writing it mid-scan can't corrupt a hash
    // that hasn't been taken yet.
    let mut batch_start = 0u64;
    let mut hashes: Vec<[u8; 32]> = Vec::with_capacity(HASHES_PER_MESSAGE);
    let mut offset = 0u64;
    while offset < size {
        while let Ok(request) = requests.try_recv() {
            if !handle_restore(request, &mut file, size).await? {
                bail!("Restore ended before the scan finished");
            }
        }

        let block = read_block(&mut file, offset, block_size, size).await?;
        hashes.push(blake3::hash(&block).into());
        offset += block_size;

        if hashes.len() >= HASHES_PER_MESSAGE || offset >= size {
            sender
                .send(SnapshotApplyResponse::Hashes {
                    offset: batch_start,
                    hashes: std::mem::take(&mut hashes),
                })
                .await?;
            batch_start = offset;
        }
    }
    sender.send(SnapshotApplyResponse::HashesDone).await?;

    while let Some(request) = requests.recv().await {
        if !handle_restore(request, &mut file, size).await? {
            break;
        }
    }
    file.sync_all().await?;
    sender.send(SnapshotApplyResponse::Applied).await?;
    debug!(?path, "Partition restore complete");
    Ok(())
}

/// Write one restored block. Returns false once the server has sent everything.
async fn handle_restore(request: SnapshotApplyRequest, file: &mut File, size: u64) -> Result<bool> {
    match request {
        SnapshotApplyRequest::Block { offset, data } => {
            let block = zstd::bulk::decompress(&data, SNAPSHOT_BLOCK_SIZE as usize)?;
            if offset >= size || offset + block.len() as u64 > size {
                bail!("Restored block is out of range");
            }
            file.seek(SeekFrom::Start(offset)).await?;
            file.write_all(&block).await?;
            Ok(true)
        }
        SnapshotApplyRequest::Done => Ok(false),
        SnapshotApplyRequest::Start { .. } => bail!("Stream already started"),
    }
}

/// Wipe free space on the given filesystem by filling it with zeros, which can
/// significantly reduce the size of subsequent snapshots.
///
/// This belongs to the *regular* agent: it must run while the filesystem is
/// still mounted, before the machine reboots into a cold snapshot. Don't use it
/// with software-based encryption schemes — the zeros are encrypted into
/// incompressible noise, making snapshots larger rather than smaller.
///
/// Not yet wired into any stream or service.
#[allow(dead_code)]
pub async fn wipe_free<P>(path: P) -> Result<()>
where
    P: AsRef<Path>,
{
    let path = path.as_ref().join(".blank");
    let mut file = File::create(&path).await?;
    let zeros = vec![0u8; SNAPSHOT_BLOCK_SIZE as usize];

    // Fill until the filesystem refuses more, then release it all.
    while file.write_all(&zeros).await.is_ok() {}

    file.sync_all().await?;
    drop(file);
    tokio::fs::remove_file(&path).await?;
    Ok(())
}
