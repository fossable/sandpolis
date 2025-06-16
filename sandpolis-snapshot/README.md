## `sandpolis-snapshot`

Allows bootagents to create/restore cold snapshots while an agent is "powered
off". Regular agents cannot manage snapshots because, unless the filesystem
itself has snapshot support, the agent is likely to produce inconsistent results
(which are probably useless).

In order to get consistent snapshots, downtime is required, so this feature
isn't suitable for high-availability systems.

Snapshots are block-based which means they work with software encryption schemes
such as LUKS and on filesystems that don't natively support snapshots.
