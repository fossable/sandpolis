## `sandpolis-snapshot`

Cold snapshots: block-by-block images of a partition, created and restored
while the machine can tolerate the partition being quiescent. Regular agents
are allowed to run them today, but the intended operator is a bootagent —
unless the filesystem itself has snapshot support, imaging a live filesystem
produces inconsistent (probably useless) results.

In order to get consistent snapshots, downtime is required, so this feature
isn't suitable for high-availability systems.

Snapshots are block-based which means they work with software encryption
schemes such as LUKS and on filesystems that don't natively support snapshots.

## Protocol

Both directions are one stream in which the agent asynchronously hashes every
block (1 MiB, blake3) and streams the hashes to the server, which compares them
against a staging image reconstructed from the stored chain:

- **Create**: the server replies with the offsets whose blocks it needs, and
  the agent uploads them zstd-compressed. A base capture compares against an
  all-zero image, so unwritten regions transfer nothing.
- **Apply**: the server replies with the blocks that differ, and the agent
  writes them back to the partition.

Clients trigger operations through a management stream to the server
(`snapshot:manage` permission); the server opens the block stream toward the
agent. The block streams carry no client permission on purpose, so only
servers can open them.

## Storage

`<data>/snapshots/<agent instance id>/<partition uuid>/<snapshot uuid>.qcow2`

Each snapshot is one qcow2 layer with zstd-compressed clusters, produced by
`qemu-img convert` (which must be on the server's PATH; the nix packages
provide it). The first snapshot is a standalone base image; every later one is
an overlay backed by its predecessor via a relative backing-file name, so only
changed clusters are stored and the chain stays relocatable. Snapshot
*metadata* lives in the realm database (`SnapshotData`, `SnapshotOperationData`)
and replicates to clients; the image bytes never do.
