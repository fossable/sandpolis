
### RQ_Execute
Request to execute a command snippet in a shell

| Field | Type | Description |
|-------|------|-------------|
| `shell_path` | `string` | The path to the shell executable |
| `command` | `string` | The command to execute |
| `timeout` | `int32` | An execution timeout in seconds |
| `ignore_stdout` | `bool` | Whether stdout will be ignored |
| `ignore_stderr` | `bool` | Whether stderr will be ignored |

### RS_Execute
Response containing execution results

| Field | Type | Description |
|-------|------|-------------|
| `exitCode` | `int32` | The process's exit code |
| `stdout` | `string` | The process's entire stdout |
| `stderr` | `string` | The process's entire stderr |

### RQ_ListShells
Request to locate supported shells on the system

| Field | Type | Description |
|-------|------|-------------|

### RS_ListShells
Response containing supported shell information

| Field | Type | Description |
|-------|------|-------------|
| `shell` | `DiscoveredShell` | null |

### RQ_ShellStream
Request to start a new shell session

| Field | Type | Description |
|-------|------|-------------|
| `stream_id` | `int32` | The desired stream ID |
| `path` | `string` | The path to the shell executable |
| `environment` | `string` | Additional environment variables |
| `rows` | `int32` | The number of rows to request |
| `cols` | `int32` | The number of columns to request |

### RS_ShellStream
null

| Field | Description |
|-------|-------------|
| SHELL_STREAM_OK | 0 |

### EV_ShellStreamInput
Event containing standard-input to a shell

| Field | Type | Description |
|-------|------|-------------|
| `stdin` | `bytes` | The input data |
| `rows_changed` | `int32` | Update the number of rows |
| `cols_changed` | `int32` | Update the number of columns |

### EV_ShellStreamOutput
Event containing standard-output and standard-error

| Field | Type | Description |
|-------|------|-------------|
| `stdout` | `bytes` | The process standard-output |
| `stderr` | `bytes` | The process standard-error |
