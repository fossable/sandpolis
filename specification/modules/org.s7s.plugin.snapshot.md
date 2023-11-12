# Snapshot Plugin

The snapshot plugin gives agents the ability to take and apply snapshots, even
on filesystems that don't natively support snapshots.

## Block-based snapshots

The target device is divided into blocks of variable size (power of 2 only).
Each block has a corresponding block hash which is the murmur3 128-bit hash of
its content.

## File-based snapshots

## Partition size considerations

### On-premise Server

(1 TiB) = (100 MiB / s) \* t

t = 1000 s ~ 16 minutes

### Off-premise Server

(1 TiB) = (1 MiB / s) \* t

t = 100000 s ~ 27 hours

## Server

The server is responsible for storing snapshot data and uploading/downloading it
to/from agents.

### Snapshot format

Snapshot contents are stored in QEMU qcow2 files on the server. This format is
mature and supports useful features like compression and encryption.

## Boot Agent

All snapshot read/write operations are run from a boot agent rather than the
regular agent. This ensures snapshots are perfectly consistent, but implies some
amount of downtime during the operation.

#### Create Snapshot

If there exist no previous snapshots, the agent first determines the
appropriate block size for the disk. The agent may take into account the size of
the disk or the erase-block size of an SSD, but the block size must be a power
of two.

If allowed, the agent will wipe the disk's free space before continuing.
This can significantly decrease the size of the resulting snapshot because empty
blocks are omitted.

If there exists a previous snapshot for the disk, the agent receives a
stream of block hashes. A single worker thread reads blocks from the disk and
compares their hashes against the block hashes retrieved from the server. If the
hashes do not match, the block is passed into a send queue to be egressed to the
server.

#### Apply Snapshot

If there exists a previous snapshot for the disk, the agent initiates a
stream of block hashes. A single worker thread reads blocks from the disk and
passes their hashes into a send queue to be egressed to the server.

Simultaneously, the agent receives a stream of block data which are placed
into a write queue to be written to the device.

## Permissions list

| Permission              | Description                                            |
| ----------------------- | ------------------------------------------------------ |
| `agent.snapshot.create` | Rights create new snapshots of agent disks             |
| `agent.snapshot.apply`  | Rights apply existing snapshots to agent disks         |
| `server.snapshot.list`  | Rights to list existing snapshots stored by the server |

## Configuration

| Property                               | Default      | Description                 |
| -------------------------------------- | ------------ | --------------------------- |
| `s7s.snapshot.storage.provider`        | _filesystem_ | The storage provider to use |
| `s7s.snapshot.storage.filesystem.path` |              | The filesystem path         |
