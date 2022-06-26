## Sandpolis Snapshot Plugin

_This plugin module is a part of
[Sandpolis](https://github.com/sandpolis/sandpolis)._

The snapshot plugin provides the ability to take and restore cold snapshots of
agent disks.

#### Snapshot Format

```
+===================+
| Snapshot Metadata |
+-------------------+
| Level 0 Hashes    |
+-------------------+
|        ...        |
+-------------------+
| Level N Hashes    |
+-------------------+
| Blocks            |
+===================+
```

##### Metadata Header

- Data size (uint64)
- Block count (uint32)
- Block size (uint16)
- Reduction Factor (uint16)
