## `sandpolis-snapshot`

Allows bootagents to create/restore cold snapshots while an agent is "powered
off". Regular agents cannot manage snapshots because, unless the filesystem
itself has snapshot support, the agent is likely to produce inconsistent results
(which are probably useless).

In order to get consistent snapshots, downtime is required, so this feature
isn't suitable for high-availability systems.

Snapshots are block-based which means they work with software encryption schemes
such as LUKS and on filesystems that don't natively support snapshots.

## Installation

| Architecture | ESP Path                |
| ------------ | ----------------------- |
| X86_64       | `/EFI/Boot/S7Sx64.efi`  |
| AArch64      | `/EFI/Boot/S7Saa64.efi` |

### Boot Wait

```
 ┌────────────────────────────────────────────────┐
 │                                                │
 │              ┌──────────────────┐              │
 │              │      Image       │              │
 │              │                  │              │
 │              └──────────────────┘              │
 │                                                │
 │                Current Status                  │
 │                                                │
 │                                                │
 │                                                │
 └────────────────────────────────────────────────┘
```

### Snapshot Operation

During a snapshot operation, the bootagent displays a visualization of what
blocks have been transferred.

```
 ┌────────────────────────────────────────────────┐
 │:Block:Visualizer:::::::::::::::::::::::::::::::│
 │::::::::::::::::::::::::::::::::::::::::::::::::│
 │::::::::::::::┌──────────────────┐::::::::::::::│
 │::::::::::::::│      Image       │::::::::::::::│
 │::::::::::::::├──────────────────┤::::::::::::::│
 │::::::::::::::│ Transfer Stats   │::::::::::::::│
 │::::::::::::::│                  │::::::::::::::│
 │::::::::::::::└──────────────────┘::::::::::::::│
 │::::::::::::::::::::::::::::::::::::::::::::::::│
 │::::::::::::::::::::::::::::::::::::::::::::::::│
 └────────────────────────────────────────────────┘
```
