
### RQ_CreateSnapshot
Create a new snapshot on a target agent.

Sources      : client
Destinations : server

| Field | Type | Description |
|-------|------|-------------|
| `agent_uuid` | `string` | The target agent's UUID |
| `partition_uuid` | `string` | The target partition's UUID |

### RQ_ApplySnapshot
Apply an existing snapshot on a target agent.

Sources      : client
Destinations : server

| Field | Type | Description |
|-------|------|-------------|
| `agent_uuid` | `string` | The target agent's UUID |
| `partition_uuid` | `string` | The target partition's UUID |
| `snapshot_uuid` | `string` | The snapshot's UUID |

### RQ_SnapshotStream
Create a new snapshot stream.

Sources      : server, agent
Destinations : server, agent

| Field | Type | Description |
|-------|------|-------------|
| `stream_id` | `int32` | The stream's ID |
| `operation` | `SnapshotOperation` | The snapshot operation type |
| `partition_uuid` | `string` | The target partition uuid |
| `block_size` | `int32` | The block size in bytes |

### EV_SnapshotDataBlock
An event containing compressed snapshot data.

Sources      : server, agent
Destinations : server, agent

| Field | Type | Description |
|-------|------|-------------|
| `offset` | `int64` | The block's offset |
| `data` | `bytes` | The block's contents compressed with zlib |

### EV_SnapshotHashBlock
An event containing one or more contiguous block hashes.

Sources      : server, agent
Destinations : server, agent

| Field | Type | Description |
|-------|------|-------------|
| `offset` | `int64` | The offset of the block that the first hash corresponds |
| `hash` | `bytes` | A list of consecutive block hashes |
