
### RQ_DirectoryStream
Start a new stream that will receive file/directory updates.

| Field | Type | Description |
|-------|------|-------------|
| `stream_id` | `int32` | null |
| `path` | `string` | The initial directory path |
| `include_sizes` | `bool` | Indicates whether file sizes should be included |
| `include_create_timestamps` | `bool` | Indicates whether creation timestamps should be included |
| `include_modify_timestamps` | `bool` | Indicates whether modification timestamps should be included |
| `include_access_timestamps` | `bool` | Indicates whether access timestamps should be included |
| `include_mime_types` | `bool` | Indicates whether MIME types should be included |
| `include_owners` | `bool` | Indicates whether file owners should be included |
| `include_groups` | `bool` | Indicates whether file groups should be included |

### RS_DirectoryStream
Response to a directory stream request.

| Field | Description |
|-------|-------------|
| DIRECTORY_STREAM_OK | 0 |
| DIRECTORY_STREAM_FAILED_PATH_NOT_EXISTS | 1 |

### EV_DirectoryStream
Updates to a directory stream.

| Field | Type | Description |
|-------|------|-------------|
| `path` | `string` | The directory's absolute path |
| `entry` | `DirectoryEntry` | Listing updates |

### RQ_MountStreamFuse
null

| Field | Type | Description |
|-------|------|-------------|
| `stream_id` | `int64` | null |
| `path` | `string` | The directory's absolute path |

### RS_MountStreamFuse
null

| Field | Description |
|-------|-------------|
| MOUNT_STREAM_OK | 0 |

### EV_MountStreamFuse
null

| Field | Type | Description |
|-------|------|-------------|

### RQ_DeleteFile
Request for one or more files to be deleted.

| Field | Type | Description |
|-------|------|-------------|
| `target` | `string` | A list of absolute paths to delete |

### RS_DeleteFile
null

| Field | Description |
|-------|-------------|
| DELETE_FILE_OK | 0 |
