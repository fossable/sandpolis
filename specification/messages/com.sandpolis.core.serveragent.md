
### RQ_AgentMetadata
Request metadata from an agent.

Sources      : server
Destinations : agent

| Field | Type | Description |
|-------|------|-------------|

### RS_AgentMetadata
Response containing agent metadata.

Sources      : agent
Destinations : server

| Field | Type | Description |
|-------|------|-------------|
| `hostname` | `string` | The network hostname |
| `os` | `core.foundation.OsType` | The OS family |
| `arch` | `core.foundation.ArchType` | The system architecture |
| `boot_agent_detected` | `bool` | Whether a boot agent was detected on the system |

### RQ_LaunchBootAgent
Request that the boot agent be started.

Sources      : client, server
Destinations : agent

| Field | Type | Description |
|-------|------|-------------|
| `target_uuid` | `string` | The UUID of the partition containing the boot agent |

### RS_LaunchBootAgent
null

| Field | Description |
|-------|-------------|
| LAUNCH_BOOT_AGENT_OK | 0 |
| LAUNCH_BOOT_AGENT_ACCESS_DENIED | 1 |

### RQ_UninstallBootAgent
Request a boot agent be uninstalled from the system.

Sources      : client, server
Destinations : agent

| Field | Type | Description |
|-------|------|-------------|
| `target_uuid` | `string` | The UUID of the partition containing the boot agent |

### RS_UninstallBootAgent
null

| Field | Description |
|-------|-------------|
| UNINSTALL_BOOT_AGENT_OK | 0 |
| UNINSTALL_BOOT_AGENT_ACCESS_DENIED | 1 |

### RQ_FindBootAgents
Request to locate all installed boot agents.

Sources      : client, server
Destinations : agent

| Field | Type | Description |
|-------|------|-------------|

### RS_FindBootAgents
Response listing boot agent installations.

Sources      : client, server
Destinations : agent

| Field | Type | Description |
|-------|------|-------------|
| `installation` | `BootAgentInstallation` | null |

### RQ_InstallBootAgent
Request a boot agent be installed on the system.

Sources      : client, server
Destinations : agent

| Field | Type | Description |
|-------|------|-------------|
| `partition_uuid` | `string` | The UUID of the target partition |
| `interface_mac` | `string` | The MAC address of the network interface to use for connections |
| `use_dhcp` | `bool` | Whether DHCP will be used |
| `static_ip` | `string` | A static IP address as an alternative to DHCP |
| `netmask` | `string` | The netmask corresponding to the static IP |
| `gateway_ip` | `string` | The gateway IP |

### RS_InstallBootAgent
null

| Field | Description |
|-------|-------------|
| INSTALL_BOOT_AGENT_OK | 0 |
| INSTALL_BOOT_AGENT_ACCESS_DENIED | 1 |

### RQ_FindBootAgentCandidates
Request candidate partitions and devices that may be suitable for a boot
agent installation.

Sources      : client, server
Destinations : agent

| Field | Type | Description |
|-------|------|-------------|

### RS_FindBootAgentCandidates
Response listing boot agent installation candidates.

Sources      : agent
Destinations : client, server
Request      : RQ_FindBootAgents

| Field | Type | Description |
|-------|------|-------------|
| `device_candidate` | `DeviceCandidate` | null |
| `partition_candidate` | `PartitionCandidate` | null |

### RQ_AuthSession
Request to authenticate the current session.

Sources      : agent
Destinations : server

| Field | Type | Description |
|-------|------|-------------|
| `password` | `string` | The group password |
| `token` | `string` | The authentication token |

### RS_AuthSession
null

| Field | Description |
|-------|-------------|
| AUTH_SESSION_OK | 0 |
| AUTH_SESSION_FAILED | 1 |

### RQ_RefreshAuthCertificate
Request to refresh an agent's authentication certificate.

Sources      : server
Destinations : agent

| Field | Type | Description |
|-------|------|-------------|
| `certificate` | `bytes` | The new "client" certificate |

### RQ_ChangePowerState
Request that the agent alter its power state.

| Field | Type | Description |
|-------|------|-------------|
| `new_state` | `PowerState` | null |
