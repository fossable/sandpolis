
### RQ_Session
Request that a new session be created. Any previous sessions associated with the
instance are invalidated.

| Field | Type | Description |
|-------|------|-------------|
| `instance_uuid` | `string` | The UUID of the requesting instance |
| `instance_type` | `core.instance.InstanceType` | The instance type of the requesting instance |
| `instance_flavor` | `core.instance.InstanceFlavor` | The instance flavor of the requesting instance |

### RS_Session
Respond to a session request with a successful result.

| Field | Type | Description |
|-------|------|-------------|
| `instance_sid` | `int32` | A SID for the requesting instance |
| `server_sid` | `int32` | The SID of the server |
| `server_uuid` | `string` | The UUID of the server |

### RQ_DirectConnection
Request the server for a new direct connection.

| Field | Type | Description |
|-------|------|-------------|
| `sid` | `int32` | The requested node |
| `port` | `int32` | An optional listener port. If specified, the requested node will attempt
a connection on this port. Otherwise, the server will coordinate the connection. |

### RQ_CoordinateConnection
Request that the recieving instance establish a new connection to the given host.

| Field | Type | Description |
|-------|------|-------------|
| `host` | `string` | The host IP address |
| `port` | `int32` | The port |
| `transport` | `string` | The transport protocol type |
| `encryption_key` | `bytes` | The initial encryption key for the new connection. |

### EV_NetworkChange
Indicates that some node in the network has changed in connection status.

| Field | Type | Description |
|-------|------|-------------|
| `node_added` | `NodeAdded` | null |
| `node_removed` | `NodeRemoved` | null |
| `connection_added` | `LinkAdded` | null |
| `connection_removed` | `LinkRemoved` | null |

### RQ_StopStream
null

| Field | Type | Description |
|-------|------|-------------|
| `id` | `int32` | The stream ID of the stream to stop |

### RS_StopStream
null

| Field | Description |
|-------|-------------|
| STOP_STREAM_OK | 0 |
| STOP_STREAM_INVALID | 1 |
