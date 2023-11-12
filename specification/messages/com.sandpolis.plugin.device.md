
### RQ_FindSubagents
Request that the receiver scan its local network for

| Field | Type | Description |
|-------|------|-------------|
| `network` | `string` | If specified, the search will be restricted to the given networks (CIDR) |
| `communicator` | `CommunicatorType` | If specified, the search will be restricted to the given communicator types |

### RS_FindSubagents
null

| Field | Type | Description |
|-------|------|-------------|
| `ssh_device` | `SshDevice` | null |
| `snmp_device` | `SnmpDevice` | null |
| `ipmi_device` | `IpmiDevice` | null |
| `http_device` | `HttpDevice` | null |
| `onvif_device` | `OnvifDevice` | null |
| `rtsp_device` | `RtspDevice` | null |
| `wol_device` | `WolDevice` | null |

### RQ_RegisterSubagent
null

| Field | Type | Description |
|-------|------|-------------|
| `ip_address` | `string` | null |
| `mac_address` | `string` | null |
| `gateway_uuid` | `string` | The uuid of the gateway instance |

### RS_RegisterSubagent
null

| Field | Description |
|-------|-------------|
| REGISTER_SUBAGENT_OK | 0 |

### RQ_IpmiCommand
Request an IPMI command be executed

| Field | Type | Description |
|-------|------|-------------|
| `command` | `string` | The IPMI command |

### RQ_SnmpWalk
Request an SNMP walk operation be executed

| Field | Type | Description |
|-------|------|-------------|
| `oid` | `string` | The OID to retrieve |

### RS_SnmpWalk
Response containing the result of a walk operation

| Field | Type | Description |
|-------|------|-------------|
| `data` | `Data` | null |

### RQ_SendWolPacket
null

| Field | Type | Description |
|-------|------|-------------|

### RS_SendWolPacket
null

| Field | Description |
|-------|-------------|
| SEND_WOL_PACKET_OK | 0 |
