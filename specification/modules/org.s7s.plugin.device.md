# Device Plugin

The device plugin extends management functionality out to agent-less devices.

## Subagents

Subagents are devices that do not have Sandpolis agent software installed, but
are instead managed via a third-party protocol such as SSH, IPMI, or SNMP from
an instance called the _gateway_. The gateway instance for a subagent may be an
independent agent or a server.

### Communicators

A subagent communicates to its gateway instance over one of the following
well-known protocols. Since subagents must accept incoming connections, the
gateway instance usually must reside on the same network segment.

#### WOL

The WOL communicator is able to send Wake-on-LAN magic packets to listening
devices.

#### SSH

The SSH communicator establishes SSH sessions with remote devices.

| Property          | Description         |
| ----------------- | ------------------- |
| `ssh.username`    | The SSH username    |
| `ssh.password`    | The SSH password    |
| `ssh.private_key` | The SSH private key |

#### IPMI

The IPMI communicator runs IPMI commands on remote devices.

| Property        | Description       |
| --------------- | ----------------- |
| `ipmi.username` | The IPMI username |
| `ipmi.password` | The IPMI password |

#### SNMP

The SNMP communicator reads and writes standard MIBs on remote devices.

| Property                     | Description                              |
| ---------------------------- | ---------------------------------------- |
| `snmp.version`               | The SNMP version                         |
| `snmp.community`             | The SNMP community string if version < 3 |
| `snmp.privacy.type`          |
| `snmp.privacy.secret`        |
| `snmp.authentication.type`   |
| `snmp.authentication.secret` |


### Subagent Scan

The local network can be scanned (if it's smaller than a /16) for devices that may be
candidate subagents.

-   For the `ssh` communicator, a TCP connection is attempted on port 22
-   For the `snmp` communicator, probes are sent via UDP port 161
-   For the `ipmi` communicator, probes are sent via UDP port 623
