
### RQ_DesktopList
Request for a listing of available desktops

| Field | Type | Description |
|-------|------|-------------|

### RS_DesktopList
Response containing all available desktops

| Field | Type | Description |
|-------|------|-------------|
| `desktop` | `Desktop` | null |

### RQ_DesktopStream
null

| Field | Type | Description |
|-------|------|-------------|
| `stream_id` | `int32` | The requested stream ID |
| `desktop_uuid` | `string` | The desktop to capture |
| `scale_factor` | `double` | The screen scale factor |

### RS_DesktopStream
null

| Field | Description |
|-------|-------------|
| DESKTOP_STREAM_OK | 0 |

### EV_DesktopStreamInput
null

| Field | Type | Description |
|-------|------|-------------|
| `key_pressed` | `string` |  |
| `key_released` | `string` |  |
| `key_typed` | `string` |  |
| `pointer_pressed` | `PointerButton` |  |
| `pointer_released` | `PointerButton` |  |
| `pointer_x` | `int32` | The X coordinate of the pointer |
| `pointer_y` | `int32` | The Y coordinate of the pointer |
| `scale_factor` | `double` | Scale factor |
| `clipboard` | `string` | Clipboard data |

### EV_DesktopStreamOutput
null

| Field | Type | Description |
|-------|------|-------------|
| `width` | `int32` | The width of the destination block in pixels |
| `height` | `int32` | The height of the destination block in pixels |
| `dest_x` | `int32` | The X coordinate of the destination block's top left corner |
| `dest_y` | `int32` | The Y coordinate of the destination block's top left corner |
| `source_x` | `int32` | The X coordinate of the source block's top left corner |
| `source_y` | `int32` | The Y coordinate of the source block's top left corner |
| `pixel_data` | `bytes` | The pixel data encoded according to the session's parameters |
| `clipboard` | `string` | Clipboard data |

### RQ_CaptureScreenshot
null

| Field | Type | Description |
|-------|------|-------------|
| `desktop_uuid` | `string` | The desktop to capture |

### RS_CaptureScreenshot
null

| Field | Type | Description |
|-------|------|-------------|
| `data` | `bytes` | null |
